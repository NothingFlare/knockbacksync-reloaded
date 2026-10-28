/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.manager;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatManager {

    private static final Set<UUID> COMBAT_PLAYERS = ConcurrentHashMap.newKeySet();

    private CombatManager() {
    }

    public static @NotNull Set<UUID> getPlayers() {
        return COMBAT_PLAYERS;
    }

    public static void addPlayer(@NotNull UUID uuid) {
        COMBAT_PLAYERS.add(uuid);
    }

    public static void removePlayer(@NotNull UUID uuid) {
        COMBAT_PLAYERS.remove(uuid);
    }

    public static boolean isInCombat(@NotNull UUID uuid) {
        return COMBAT_PLAYERS.contains(uuid);
    }

    public static void clear() {
        COMBAT_PLAYERS.clear();
    }
}

