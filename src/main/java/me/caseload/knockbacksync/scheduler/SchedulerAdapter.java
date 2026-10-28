/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.scheduler;

public interface SchedulerAdapter {
    AbstractTaskHandle runTask(Runnable task);
    AbstractTaskHandle runTaskAsynchronously(Runnable task);
    AbstractTaskHandle runTaskLater(Runnable task, long delayTicks);
    AbstractTaskHandle runTaskTimer(Runnable task, long delayTicks, long periodTicks);
    AbstractTaskHandle runTaskLaterAsynchronously(Runnable task, long delayTicks);
    AbstractTaskHandle runTaskTimerAsynchronously(Runnable task, long delayTicks, long periodTicks);
}

