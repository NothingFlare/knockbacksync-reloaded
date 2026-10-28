/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.listener;

import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!KnockbackSync.getInstance().getConfigManager().isToggled()) {
            return;
        }

        Entity victimEntity = event.getEntity();
        Entity attackerEntity = event.getDamager();

        if (!(victimEntity instanceof Player) || !(attackerEntity instanceof Player)) {
            return;
        }

        Player victim = (Player) victimEntity;
        Player attacker = (Player) attackerEntity;

        PlayerData playerData = PlayerDataManager.getPlayerData(victim.getUniqueId());
        if (playerData == null) {
            return;
        }

        playerData.setVerticalVelocity(playerData.calculateVerticalVelocity(attacker));
        playerData.setLastDamageTicks(victim.getNoDamageTicks());
        playerData.updateCombat();

        if (!KnockbackSync.getInstance().getConfigManager().isRunnableEnabled()) {
            playerData.sendPing();
        }
    }
}

