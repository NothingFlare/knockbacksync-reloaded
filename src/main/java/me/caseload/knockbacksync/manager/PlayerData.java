/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import lombok.Setter;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.util.CollisionUtil;
import me.caseload.knockbacksync.util.MathUtil;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PlayerData {

    private final Player player;
    private volatile User user;

    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);

    // In-flight ping tracking (primitive, zero allocation, high-resolution nanoTime)
    private static final long PING_TIMEOUT_NANOS = 3_000_000_000L; // 3 seconds timeout
    private volatile int inFlightId1 = -1;
    private volatile long inFlightTime1 = 0L;
    private volatile int inFlightId2 = -1;
    private volatile long inFlightTime2 = 0L;

    // Ping smoothing ring buffer & scratch buffer (zero allocations on median calculation)
    private long[] pingHistory;
    private long[] sortBuffer;
    private int historyIndex = 0;
    private int samplesCount = 0;
    private volatile long lastPing = -1L;
    private volatile long cachedMedianPing = -1L;

    // Combat tracking via timestamp (eliminates per-hit scheduler task churn)
    private volatile long combatExpiryTime = 0L;

    @Nullable @Setter
    private Double verticalVelocity;

    @Nullable @Setter
    private Integer lastDamageTicks;

    private volatile boolean pendingKnockback = false;
    private volatile long pendingKnockbackExpiry = 0L;

    @Setter
    private double gravity = 0.08;

    public PlayerData(@NotNull Player player) {
        this.player = player;
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        int sampleSize = KnockbackSync.getInstance().getConfigManager().getSampleSize();
        this.pingHistory = new long[sampleSize];
        this.sortBuffer = new long[sampleSize];

        // Seed initial latency to eliminate cold-start delay
        long initialPing = 20L;
        try {
            initialPing = Math.max(1L, player.getPing());
        } catch (Throwable ignored) {
        }
        this.cachedMedianPing = initialPing;
        this.lastPing = initialPing;
        this.pingHistory[0] = initialPing;
        this.historyIndex = 1;
        this.samplesCount = 1;
    }

    public @Nullable User getUser() {
        if (this.user != null) {
            return this.user;
        }
        User fetched = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (fetched != null) {
            this.user = fetched;
        }
        return fetched;
    }

    public void setPendingKnockback(boolean pending) {
        this.pendingKnockback = pending;
        this.pendingKnockbackExpiry = pending ? System.currentTimeMillis() + 150L : 0L;
    }

    public boolean isPendingKnockback() {
        if (!pendingKnockback) {
            return false;
        }
        if (System.currentTimeMillis() > pendingKnockbackExpiry) {
            this.pendingKnockback = false;
            this.verticalVelocity = null;
            return false;
        }
        return true;
    }

    /**
     * Reinitializes smoothing buffers if sample size changed during reload.
     */
    public synchronized void resizeBuffer(int newSize) {
        if (pingHistory.length == newSize) return;
        long[] newHistory = new long[newSize];
        long[] newSort = new long[newSize];
        int countToCopy = Math.min(samplesCount, newSize);
        System.arraycopy(pingHistory, 0, newHistory, 0, countToCopy);
        this.pingHistory = newHistory;
        this.sortBuffer = newSort;
        this.historyIndex = countToCopy % newSize;
        this.samplesCount = countToCopy;
    }

    /**
     * Registers an in-flight ping packet in primitive slots. Returns true if accepted, false if throttled.
     */
    public synchronized boolean registerInFlightPing(int packetId, long nowNanos) {
        if (inFlightId1 != -1 && (nowNanos - inFlightTime1 > PING_TIMEOUT_NANOS)) {
            inFlightId1 = -1;
        }
        if (inFlightId2 != -1 && (nowNanos - inFlightTime2 > PING_TIMEOUT_NANOS)) {
            inFlightId2 = -1;
        }

        if (inFlightId1 == -1) {
            inFlightId1 = packetId;
            inFlightTime1 = nowNanos;
            return true;
        } else if (inFlightId2 == -1) {
            inFlightId2 = packetId;
            inFlightTime2 = nowNanos;
            return true;
        }
        return false;
    }

    /**
     * Resolves in-flight ping and returns elapsed RTT in milliseconds, or -1 if not found.
     */
    public synchronized long removeInFlightPing(int packetId) {
        long nowNanos = System.nanoTime();
        if (inFlightId1 == packetId) {
            long send = inFlightTime1;
            inFlightId1 = -1;
            return Math.max(1L, (nowNanos - send) / 1_000_000L);
        } else if (inFlightId2 == packetId) {
            long send = inFlightTime2;
            inFlightId2 = -1;
            return Math.max(1L, (nowNanos - send) / 1_000_000L);
        }
        return -1L;
    }

    public synchronized void clearInFlightPings() {
        inFlightId1 = -1;
        inFlightTime1 = 0L;
        inFlightId2 = -1;
        inFlightTime2 = 0L;
    }

    /**
     * Verifies that the underlying network channel is open, active, writable, and in PLAY state.
     */
    public boolean isChannelHealthy() {
        if (!player.isOnline() || player.isDead()) {
            return false;
        }
        User u = getUser();
        if (u == null) {
            return false;
        }
        if (u.getConnectionState() != ConnectionState.PLAY) {
            return false;
        }
        Object channelObj = u.getChannel();
        if (!(channelObj instanceof Channel)) {
            return false;
        }
        Channel channel = (Channel) channelObj;
        return channel.isOpen() && channel.isActive() && channel.isWritable();
    }

    /**
     * Sends packet using Netty's voidPromise and direct channel pipeline writing.
     * Transforming WrapperPlayServerPing into raw packet ByteBuf/NMS and passing Netty's voidPromise
     * ensures Netty's ChannelOutboundBuffer.failFlushed releases buffers silently on socket closure
     * without producing StacklessClosedChannelException warnings in server logs.
     */
    private void sendPingPacketSafely(@NotNull WrapperPlayServerPing packet) {
        User u = getUser();
        if (u == null) return;

        Object channelObj = u.getChannel();
        if (!(channelObj instanceof Channel)) {
            try {
                u.sendPacketSilently(packet);
            } catch (Throwable ignored) {
            }
            return;
        }

        Channel channel = (Channel) channelObj;
        if (!channel.isOpen() || !channel.isActive()) {
            return;
        }

        try {
            // 1. Transform wrapper into raw NMS / ByteBuf packet objects
            Object[] rawPackets = PacketEvents.getAPI().getProtocolManager().transformWrappers(packet, channel, true);
            if (rawPackets == null || rawPackets.length == 0) {
                return;
            }

            // 2. Locate PacketEvents encoder context in the Netty pipeline
            ChannelPipeline pipeline = channel.pipeline();
            ChannelHandlerContext ctx = pipeline.context(PacketEvents.ENCODER_NAME);

            if (ctx != null) {
                for (Object rawPacket : rawPackets) {
                    if (rawPacket == null) continue;
                    ctx.writeAndFlush(rawPacket, ctx.voidPromise());
                }
            } else {
                for (Object rawPacket : rawPackets) {
                    if (rawPacket == null) continue;
                    channel.writeAndFlush(rawPacket, channel.voidPromise());
                }
            }
        } catch (Throwable ignored) {
            // Channel may have closed concurrently; safely ignore
        }
    }

    /**
     * Sends a ping packet to measure round-trip latency safely with high-precision monotonic timing.
     * Includes in-flight throttling and channel health validation.
     */
    public void sendPing() {
        if (!isChannelHealthy()) {
            quitCombat();
            return;
        }

        int packetId = packetIdGenerator.updateAndGet(id -> (id >= 32700) ? 1 : id + 1);
        long nowNanos = System.nanoTime();

        // Throttle check
        if (!registerInFlightPing(packetId, nowNanos)) {
            return;
        }

        try {
            WrapperPlayServerPing packet = new WrapperPlayServerPing(packetId);
            sendPingPacketSafely(packet);
        } catch (Throwable ignored) {
            removeInFlightPing(packetId);
        }
    }

    /**
     * Records a new ping sample with outlier filtering and updates running median without memory allocations.
     */
    public synchronized void recordPingSample(long rawRtt) {
        ConfigManager config = KnockbackSync.getInstance().getConfigManager();
        long spikeThreshold = config.getSpikeThreshold();

        long sample = rawRtt;
        long baseline = cachedMedianPing > 0 ? cachedMedianPing : (lastPing > 0 ? lastPing : sample);

        long diff = sample - baseline;
        if (diff > spikeThreshold) {
            sample = baseline + (spikeThreshold / 2);
        } else if (-diff > spikeThreshold) {
            sample = baseline - (spikeThreshold / 2);
        }

        lastPing = sample;
        pingHistory[historyIndex] = sample;
        historyIndex = (historyIndex + 1) % pingHistory.length;
        if (samplesCount < pingHistory.length) {
            samplesCount++;
        }

        // Fast zero-allocation insertion sort for running median
        System.arraycopy(pingHistory, 0, sortBuffer, 0, samplesCount);
        for (int i = 1; i < samplesCount; i++) {
            long key = sortBuffer[i];
            int j = i - 1;
            while (j >= 0 && sortBuffer[j] > key) {
                sortBuffer[j + 1] = sortBuffer[j];
                j--;
            }
            sortBuffer[j + 1] = key;
        }
        cachedMedianPing = sortBuffer[samplesCount / 2];
    }

    /**
     * Gets the smoothed median ping. Fallback to Bukkit's getPing() if no samples recorded yet.
     */
    public long getSmoothedPing() {
        if (cachedMedianPing > 0) {
            return cachedMedianPing;
        }
        try {
            return Math.max(1, player.getPing());
        } catch (Throwable ignored) {
            return 20;
        }
    }

    /**
     * Calculates the effective ping with base offset and tolerance buffer.
     * If ping is below min_ping, returns 0 (skips unnecessary compensation).
     */
    public long getEffectivePing() {
        ConfigManager config = KnockbackSync.getInstance().getConfigManager();
        long smoothed = getSmoothedPing();

        if (smoothed < config.getMinPing()) {
            return 0;
        }

        long effective = smoothed - config.getPingOffset() + config.getPingTolerance();
        return MathUtil.clamp(effective, 0L, config.getMaxPing());
    }

    /**
     * Evaluates whether the player has already touched the ground on their client
     * based on vertical trajectory, distance to ground, and network latency.
     */
    public boolean isOnGround(double currentVerticalVelocity) {
        if (player.isGliding() || player.isSwimming() || player.isRiptiding()) {
            return false;
        }
        if (player.hasPotionEffect(PotionEffectType.LEVITATION) || player.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            return false;
        }

        Material material = player.getLocation().getBlock().getType();
        if (material == Material.WATER || material == Material.LAVA
                || material == Material.COBWEB || material == Material.SCAFFOLDING
                || material == Material.BUBBLE_COLUMN || material == Material.POWDER_SNOW) {
            return false;
        }

        long effectivePing = getEffectivePing();
        if (effectivePing <= 0) {
            return false;
        }

        ConfigManager config = KnockbackSync.getInstance().getConfigManager();
        double maxDist = config.getMaxGroundDistance();

        double gDist = config.isFastGroundCheck()
                ? CollisionUtil.getFastDistanceToGround(player, maxDist)
                : CollisionUtil.getRayTraceDistanceToGround(player, maxDist);

        // If distance is negative (no solid ground found), essentially zero, or beyond max distance
        if (gDist < 0.0 || gDist <= 0.001 || gDist > maxDist) {
            return false;
        }

        int tMax = currentVerticalVelocity > 0.0
                ? MathUtil.calculateTimeToMaxVelocity(currentVerticalVelocity, gravity)
                : 0;
        double mH = currentVerticalVelocity > 0.0
                ? MathUtil.calculateDistanceTraveled(currentVerticalVelocity, tMax, gravity)
                : 0.0;

        // If player is moving upward, downward fall starts from apex where velocity is 0.0.
        // If player is already moving downward (or stationary), fall starts with that downward speed.
        double fallStartVelocity = currentVerticalVelocity > 0.0 ? 0.0 : -currentVerticalVelocity;
        int tFall = MathUtil.calculateFallTime(fallStartVelocity, mH + gDist, gravity);

        if (tFall == -1) {
            return false;
        }

        // Required time in ms for the client to have finished falling to the ground
        long requiredTimeMs = (long) Math.ceil((tMax + tFall) * 50.0);

        return effectivePing >= requiredTimeMs;
    }

    /**
     * Calculates the vanilla vertical knockback velocity imparted by the attacker.
     */
    public double calculateVerticalVelocity(@NotNull Player attacker) {
        double yAxis = attacker.getAttackCooldown() > 0.848 ? 0.4 : 0.36080000519752503;

        if (!attacker.isSprinting()) {
            yAxis = 0.36080000519752503;
        }

        if (attacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK) > 0) {
            yAxis = 0.4;
        }

        // Apply knockback resistance uniformly
        AttributeInstance knockbackAttr = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockbackAttr != null) {
            double knockbackResistance = knockbackAttr.getValue();
            if (knockbackResistance > 0.0) {
                double factor = MathUtil.clamp(1.0 - knockbackResistance, 0.0, 1.0);
                yAxis *= factor;
            }
        }

        return yAxis;
    }

    public boolean isInCombat() {
        return System.currentTimeMillis() < combatExpiryTime;
    }

    public void updateCombat() {
        boolean wasInCombat = isInCombat();
        long durationMs = KnockbackSync.getInstance().getConfigManager().getCombatTimer() * 50L;
        combatExpiryTime = System.currentTimeMillis() + durationMs;
        CombatManager.addPlayer(player.getUniqueId());

        // Instant responsiveness: if entering combat, send an immediate ping
        if (!wasInCombat) {
            sendPing();
        }
    }

    public void quitCombat() {
        combatExpiryTime = 0L;
        CombatManager.removePlayer(player.getUniqueId());
        clearInFlightPings();
    }

    public ClientVersion getClientVersion() {
        User u = getUser();
        if (u != null) {
            ClientVersion ver = u.getClientVersion();
            if (ver != null) return ver;
        }
        try {
            return ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion());
        } catch (Throwable ignored) {
            return ClientVersion.V_1_18_2;
        }
    }
}
