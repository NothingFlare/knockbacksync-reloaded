/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.scheduler;

import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class BukkitTaskHandle implements AbstractTaskHandle {

    private final BukkitTask task;

    public BukkitTaskHandle(@Nullable BukkitTask task) {
        this.task = task;
    }

    @Override
    public boolean isCancelled() {
        return task == null || task.isCancelled();
    }

    @Override
    public void cancel() {
        if (task != null) {
            task.cancel();
        }
    }
}
