/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.manager;

import io.github.retrooper.packetevents.util.GeyserUtil;
import me.caseload.knockbacksync.util.FloodgateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {

    private static final Map<UUID, PlayerData> PLAYER_DATA_MAP = new ConcurrentHashMap<>();

    private PlayerDataManager() {
    }

    public static @Nullable PlayerData getPlayerData(@NotNull UUID uuid) {
        return PLAYER_DATA_MAP.get(uuid);
    }

    public static void addPlayerData(@NotNull UUID uuid, @NotNull PlayerData playerData) {
        if (!shouldExempt(uuid)) {
            PLAYER_DATA_MAP.put(uuid, playerData);
        }
    }

    public static @Nullable PlayerData removePlayerData(@NotNull UUID uuid) {
        return PLAYER_DATA_MAP.remove(uuid);
    }

    public static boolean containsPlayerData(@NotNull UUID uuid) {
        return PLAYER_DATA_MAP.containsKey(uuid);
    }

    public static boolean shouldExempt(@NotNull UUID uuid) {
        try {
            if (GeyserUtil.isGeyserPlayer(uuid)) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        if (FloodgateUtil.isFloodgatePlayer(uuid)) {
            return true;
        }

        return uuid.toString().startsWith("00000000-0000-0000-0009");
    }

    public static void clear() {
        PLAYER_DATA_MAP.clear();
    }
}

