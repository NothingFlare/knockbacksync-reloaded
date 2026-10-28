/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.manager;

import lombok.Getter;
import lombok.Setter;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.runnable.PingRunnable;
import me.caseload.knockbacksync.scheduler.AbstractTaskHandle;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
@Setter
public class ConfigManager {

    private boolean toggled = true;
    private boolean runnableEnabled = true;

    private long runnableInterval = 5L;
    private long combatTimer = 30L;

    // Ping smoothing & tolerance
    private long pingOffset = 25L;
    private long pingTolerance = 20L;
    private long minPing = 20L;
    private long maxPing = 400L;
    private long spikeThreshold = 30L;
    private int sampleSize = 5;

    // Ground & physics
    private double maxGroundDistance = 1.35;
    private boolean fastGroundCheck = true;

    // Messages
    private String enableMessage;
    private String disableMessage;
    private String playerEnableMessage;
    private String playerDisableMessage;
    private String playerIneligibleMessage;
    private String reloadMessage;

    private AbstractTaskHandle pingTask;

    public void loadConfig(boolean reloadConfig) {
        KnockbackSync plugin = KnockbackSync.getInstance();

        if (reloadConfig) {
            plugin.reloadConfig();
        }

        FileConfiguration config = plugin.getConfig();

        // Cancel running task if already active to prevent duplicates
        if (pingTask != null) {
            pingTask.cancel();
            pingTask = null;
        }

        toggled = config.getBoolean("enabled", true);
        runnableEnabled = config.getBoolean("runnable.enabled", true);
        runnableInterval = Math.max(1L, config.getLong("runnable.interval", 5L));
        combatTimer = Math.max(1L, config.getLong("runnable.combat_timer", config.getLong("runnable.timer", 30L)));

        // Ping settings with backwards compatibility for flat keys
        pingOffset = config.getLong("ping.offset", config.getLong("ping_offset", 25L));
        pingTolerance = config.getLong("ping.tolerance", 20L);
        minPing = config.getLong("ping.min_ping", 20L);
        maxPing = config.getLong("ping.max_ping", 400L);
        spikeThreshold = config.getLong("ping.spike_threshold", config.getLong("spike_threshold", 30L));
        sampleSize = Math.max(3, Math.min(15, config.getInt("ping.sample_size", 5)));

        // Physics settings
        maxGroundDistance = config.getDouble("physics.max_ground_distance", 1.35);
        fastGroundCheck = config.getBoolean("physics.fast_ground_check", true);

        // Messages
        enableMessage = config.getString("messages.enable", config.getString("enable_message", "&aSuccessfully enabled KnockbackSync globally."));
        disableMessage = config.getString("messages.disable", config.getString("disable_message", "&cSuccessfully disabled KnockbackSync globally."));
        playerEnableMessage = config.getString("messages.player_enable", config.getString("player_enable_message", "&aSuccessfully enabled KnockbackSync for &e%player%&a."));
        playerDisableMessage = config.getString("messages.player_disable", config.getString("player_disable_message", "&cSuccessfully disabled KnockbackSync for &e%player%&c."));
        playerIneligibleMessage = config.getString("messages.player_ineligible", config.getString("player_ineligible_message", "&c%player% is ineligible for KnockbackSync (Bedrock / Geyser player)."));
        reloadMessage = config.getString("messages.reload", config.getString("reload_message", "&aSuccessfully reloaded KnockbackSync configuration."));

        // Schedule ping task if enabled
        if (runnableEnabled) {
            long initialDelay = KnockbackSync.getInstance().isFolia() ? 1L : runnableInterval;
            pingTask = KnockbackSync.getInstance().getScheduler().runTaskTimerAsynchronously(
                    new PingRunnable(),
                    initialDelay,
                    runnableInterval
            );
        }
    }

    public void cleanup() {
        if (pingTask != null) {
            pingTask.cancel();
            pingTask = null;
        }
    }
}

