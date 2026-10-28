/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.manager;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPing;
import lombok.Getter;
import lombok.Setter;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.scheduler.AbstractTaskHandle;
import me.caseload.knockbacksync.util.CollisionUtil;
import me.caseload.knockbacksync.util.MathUtil;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class PlayerData {

    private final Player player;
    private final User user;

    private final Map<Integer, Long> pendingPings = new ConcurrentHashMap<>(4);
    private final AtomicInteger packetIdGenerator = new AtomicInteger(1);

    @Nullable
    private AbstractTaskHandle combatTask;

    // Ping smoothing ring buffer
    private final long[] pingHistory;
    private int historyIndex = 0;
    private int samplesCount = 0;
    private volatile long lastPing = -1;
    private volatile long cachedMedianPing = -1;

    @Nullable @Setter
    private Double verticalVelocity;

    @Nullable @Setter
    private Integer lastDamageTicks;

    @Setter
    private double gravity = 0.08;

    public PlayerData(@NotNull Player player) {
        this.player = player;
        this.user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        int sampleSize = KnockbackSync.getInstance().getConfigManager().getSampleSize();
        this.pingHistory = new long[sampleSize];
    }

    /**
     * Sends a ping packet to measure round-trip latency.
     */
    public void sendPing() {
        if (!player.isOnline()) return;

        int packetId = packetIdGenerator.updateAndGet(id -> (id >= 32700) ? 1 : id + 1);
        pendingPings.put(packetId, System.currentTimeMillis());

        WrapperPlayServerPing packet = new WrapperPlayServerPing(packetId);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }

    /**
     * Records a new ping sample with spike filtering and updates the smoothed median.
     */
    public synchronized void recordPingSample(long rawRtt) {
        ConfigManager config = KnockbackSync.getInstance().getConfigManager();
        long spikeThreshold = config.getSpikeThreshold();

        long sample = rawRtt;
        if (samplesCount > 0 && lastPing > 0) {
            long diff = sample - lastPing;
            if (diff > spikeThreshold) {
                // Lag spike detected: clamp to lastPing + threshold / 2 to avoid erratic jumps
                sample = lastPing + (spikeThreshold / 2);
            } else if (-diff > spikeThreshold) {
                sample = lastPing - (spikeThreshold / 2);
            }
        }

        lastPing = sample;
        pingHistory[historyIndex] = sample;
        historyIndex = (historyIndex + 1) % pingHistory.length;
        if (samplesCount < pingHistory.length) {
            samplesCount++;
        }

        // Compute median from valid samples
        long[] copy = Arrays.copyOf(pingHistory, samplesCount);
        Arrays.sort(copy);
        cachedMedianPing = copy[samplesCount / 2];
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
        Material material = player.getLocation().getBlock().getType();
        if (player.isGliding() || material == Material.WATER || material == Material.LAVA
                || material == Material.COBWEB || material == Material.SCAFFOLDING) {
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

        // If distance is essentially zero, the server already considers the player on the ground
        if (gDist <= 0.001 || gDist > maxDist) {
            return false;
        }

        int tMax = currentVerticalVelocity > 0 ? MathUtil.calculateTimeToMaxVelocity(currentVerticalVelocity, gravity) : 0;
        double mH = currentVerticalVelocity > 0 ? MathUtil.calculateDistanceTraveled(currentVerticalVelocity, tMax, gravity) : 0.0;
        int tFall = MathUtil.calculateFallTime(currentVerticalVelocity, mH + gDist, gravity);

        if (tFall == -1) {
            return false;
        }

        // Required time in ms for the client to have finished falling to the ground
        long requiredTimeMs = (long) Math.ceil((tMax + tFall) * 50.0);

        return effectivePing >= requiredTimeMs && gDist <= maxDist;
    }

    /**
     * Calculates the vanilla vertical knockback velocity imparted by the attacker.
     */
    public double calculateVerticalVelocity(@NotNull Player attacker) {
        double yAxis = attacker.getAttackCooldown() > 0.848 ? 0.4 : 0.36080000519752503;

        if (!attacker.isSprinting()) {
            yAxis = 0.36080000519752503;
            AttributeInstance knockbackAttr = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
            if (knockbackAttr != null) {
                double knockbackResistance = knockbackAttr.getValue();
                double resistanceFactor = 0.04000000119 * knockbackResistance * 10.0;
                yAxis -= resistanceFactor;
            }
        }

        if (attacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK) > 0) {
            yAxis = 0.4;
        }

        return yAxis;
    }

    public boolean isInCombat() {
        return combatTask != null;
    }

    public void updateCombat() {
        if (isInCombat() && combatTask != null) {
            combatTask.cancel();
        }

        CombatManager.addPlayer(player.getUniqueId());
        combatTask = KnockbackSync.getInstance().getScheduler().runTaskLaterAsynchronously(
                () -> quitCombat(false),
                KnockbackSync.getInstance().getConfigManager().getCombatTimer()
        );
    }

    public void quitCombat(boolean cancelTask) {
        if (cancelTask && combatTask != null) {
            combatTask.cancel();
        }
        combatTask = null;
        CombatManager.removePlayer(player.getUniqueId());
        pendingPings.clear();
    }

    public ClientVersion getClientVersion() {
        if (user != null) {
            ClientVersion ver = user.getClientVersion();
            if (ver != null) return ver;
        }
        try {
            return ClientVersion.getById(PacketEvents.getAPI().getServerManager().getVersion().getProtocolVersion());
        } catch (Throwable ignored) {
            return ClientVersion.V_1_18_2;
        }
    }
}

