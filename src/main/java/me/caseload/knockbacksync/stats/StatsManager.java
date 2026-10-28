/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.stats;

import me.caseload.knockbacksync.KnockbackSync;
import org.bstats.bukkit.Metrics;

public final class StatsManager {

    private static final int BSTATS_PLUGIN_ID = 23568;
    private static Metrics metrics;

    private StatsManager() {
    }

    public static void init() {
        try {
            KnockbackSync.getInstance().getScheduler().runTaskAsynchronously(() -> {
                try {
                    metrics = new Metrics(KnockbackSync.getInstance(), BSTATS_PLUGIN_ID);
                    metrics.addCustomChart(new PlayerVersionsPie());
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }
}

