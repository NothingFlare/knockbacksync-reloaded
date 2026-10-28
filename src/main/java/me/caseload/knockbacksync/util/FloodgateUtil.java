/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.util;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public final class FloodgateUtil {

    private static volatile boolean checkedForFloodgate = false;
    private static volatile boolean floodgatePresent = false;

    private FloodgateUtil() {
    }

    public static boolean isFloodgatePlayer(UUID uuid) {
        if (!checkedForFloodgate) {
            synchronized (FloodgateUtil.class) {
                if (!checkedForFloodgate) {
                    try {
                        Class.forName("org.geysermc.floodgate.api.FloodgateApi");
                        floodgatePresent = true;
                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                        floodgatePresent = false;
                    }
                    checkedForFloodgate = true;
                }
            }
        }

        if (floodgatePresent) {
            try {
                return FloodgateApi.getInstance() != null && FloodgateApi.getInstance().isFloodgatePlayer(uuid);
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }
}

