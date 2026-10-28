/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.scheduler;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

public class FoliaSchedulerAdapter implements SchedulerAdapter {
    private final Plugin plugin;
    private final GlobalRegionScheduler globalScheduler;
    private final AsyncScheduler asyncScheduler;

    public FoliaSchedulerAdapter(Plugin plugin) {
        this.plugin = plugin;
        GlobalRegionScheduler globalSched = null;
        AsyncScheduler asyncSched = null;
        try {
            globalSched = (GlobalRegionScheduler) Bukkit.getServer().getClass().getMethod("getGlobalRegionScheduler").invoke(Bukkit.getServer());
            asyncSched = (AsyncScheduler) Bukkit.getServer().getClass().getMethod("getAsyncScheduler").invoke(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize Folia scheduler via reflection: " + e.getMessage());
        }
        this.globalScheduler = globalSched;
        this.asyncScheduler = asyncSched;
    }

    @Override
    public AbstractTaskHandle runTask(Runnable task) {
        return new AbstractTaskHandle(globalScheduler.run(plugin, scheduledTask -> task.run()));
    }

    @Override
    public AbstractTaskHandle runTaskAsynchronously(Runnable task) {
        return new AbstractTaskHandle(asyncScheduler.runNow(plugin, scheduledTask -> task.run()));
    }

    @Override
    public AbstractTaskHandle runTaskLater(Runnable task, long delayTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        return new AbstractTaskHandle(globalScheduler.runDelayed(plugin, scheduledTask -> task.run(), safeDelay));
    }

    @Override
    public AbstractTaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        long safeDelay = Math.max(1L, delayTicks);
        long safePeriod = Math.max(1L, periodTicks);
        return new AbstractTaskHandle(globalScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), safeDelay, safePeriod));
    }

    @Override
    public AbstractTaskHandle runTaskLaterAsynchronously(Runnable task, long delayTicks) {
        long delayMs = Math.max(1L, delayTicks) * 50L;
        return new AbstractTaskHandle(asyncScheduler.runDelayed(plugin, scheduledTask -> task.run(), delayMs, TimeUnit.MILLISECONDS));
    }

    @Override
    public AbstractTaskHandle runTaskTimerAsynchronously(Runnable task, long delayTicks, long periodTicks) {
        long delayMs = Math.max(1L, delayTicks) * 50L;
        long periodMs = Math.max(1L, periodTicks) * 50L;
        return new AbstractTaskHandle(asyncScheduler.runAtFixedRate(plugin, scheduledTask -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS));
    }
}

