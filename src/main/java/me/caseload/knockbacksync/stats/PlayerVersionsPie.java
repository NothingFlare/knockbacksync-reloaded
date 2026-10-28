/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.stats;

import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class PlayerVersionsPie extends AdvancedPie {

    public PlayerVersionsPie() {
        super("player_version", () -> {
            Map<String, Integer> valueMap = new HashMap<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData playerData = PlayerDataManager.getPlayerData(player.getUniqueId());
                if (playerData != null && playerData.getClientVersion() != null) {
                    String ver = playerData.getClientVersion().toString();
                    valueMap.put(ver, valueMap.getOrDefault(ver, 0) + 1);
                }
            }
            return valueMap;
        });
    }
}

