/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class AbstractTaskHandle {
    private final BukkitTask bukkitTask;
    private final ScheduledTask scheduledTask;

    public AbstractTaskHandle(@Nullable BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
        this.scheduledTask = null;
    }

    public AbstractTaskHandle(@Nullable ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
        this.bukkitTask = null;
    }

    public boolean isCancelled() {
        if (this.bukkitTask != null) {
            return this.bukkitTask.isCancelled();
        }
        if (this.scheduledTask != null) {
            return this.scheduledTask.isCancelled();
        }
        return true;
    }

    public void cancel() {
        if (this.bukkitTask != null) {
            this.bukkitTask.cancel();
        } else if (this.scheduledTask != null) {
            this.scheduledTask.cancel();
        }
    }
}

