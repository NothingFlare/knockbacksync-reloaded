/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.jetbrains.annotations.Nullable;

public class FoliaTaskHandle implements AbstractTaskHandle {

    private final ScheduledTask task;

    public FoliaTaskHandle(@Nullable ScheduledTask task) {
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
