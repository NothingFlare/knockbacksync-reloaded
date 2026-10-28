/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync;

import com.github.retrooper.packetevents.PacketEvents;
import lombok.Getter;
import me.caseload.knockbacksync.command.MainCommand;
import me.caseload.knockbacksync.listener.AttributeChangeListener;
import me.caseload.knockbacksync.listener.PingReceiveListener;
import me.caseload.knockbacksync.listener.PlayerDamageListener;
import me.caseload.knockbacksync.listener.PlayerJoinQuitListener;
import me.caseload.knockbacksync.listener.PlayerKnockbackListener;
import me.caseload.knockbacksync.manager.CombatManager;
import me.caseload.knockbacksync.manager.ConfigManager;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.scheduler.BukkitSchedulerAdapter;
import me.caseload.knockbacksync.scheduler.FoliaSchedulerAdapter;
import me.caseload.knockbacksync.scheduler.SchedulerAdapter;
import me.caseload.knockbacksync.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class KnockbackSync extends JavaPlugin {

    public static Logger LOGGER;
    private static KnockbackSync instance;

    @Getter
    private SchedulerAdapter scheduler;

    @Getter
    private final boolean folia = checkFolia();

    @Getter
    private final ConfigManager configManager = new ConfigManager();

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        LOGGER = getLogger();
        instance = this;

        scheduler = folia ? new FoliaSchedulerAdapter(this) : new BukkitSchedulerAdapter(this);

        saveDefaultConfig();
        configManager.loadConfig(false);

        PluginCommand cmd = getCommand("knockbacksync");
        if (cmd != null) {
            MainCommand mainCommand = new MainCommand();
            cmd.setExecutor(mainCommand);
            cmd.setTabCompleter(mainCommand);
        }

        registerListeners(
                new PlayerDamageListener(),
                new PlayerKnockbackListener(),
                new PlayerJoinQuitListener()
        );

        PacketEvents.getAPI().getEventManager().registerListeners(
                new AttributeChangeListener(),
                new PingReceiveListener()
        );

        // Populate already online players (e.g. if plugin loaded dynamically)
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerDataManager.addPlayerData(player.getUniqueId(), new PlayerData(player));
        }

        StatsManager.init();
        LOGGER.info("KnockbackSync Reloaded v1.0 successfully enabled (Folia: " + folia + ").");
    }

    @Override
    public void onDisable() {
        configManager.cleanup();
        CombatManager.clear();
        PlayerDataManager.clear();

        LOGGER.info("KnockbackSync Reloaded disabled.");
    }

    public static KnockbackSync getInstance() {
        return instance != null ? instance : getPlugin(KnockbackSync.class);
    }

    private void registerListeners(Listener... listeners) {
        PluginManager pluginManager = getServer().getPluginManager();
        for (Listener listener : listeners) {
            pluginManager.registerEvents(listener, this);
        }
    }
}
