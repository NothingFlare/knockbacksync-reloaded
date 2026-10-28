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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public class PlayerKnockbackListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        if (!KnockbackSync.getInstance().getConfigManager().isToggled()) {
            return;
        }

        Player victim = event.getPlayer();
        EntityDamageEvent entityDamageEvent = victim.getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (!(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return;
        }

        Entity attacker = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        if (!(attacker instanceof Player)) {
            return;
        }

        PlayerData playerData = PlayerDataManager.getPlayerData(victim.getUniqueId());
        if (playerData == null) {
            return;
        }

        Integer damageTicks = playerData.getLastDamageTicks();
        if (damageTicks != null && damageTicks > 8) {
            return;
        }

        Double verticalVelocity = playerData.getVerticalVelocity();
        if (verticalVelocity == null) {
            return;
        }

        Vector velocity = event.getVelocity();
        if (!playerData.isOnGround(velocity.getY())) {
            return;
        }

        Vector adjustedVelocity = velocity.clone().setY(verticalVelocity);
        event.setVelocity(adjustedVelocity);

        // Consume the calculated vertical velocity
        playerData.setVerticalVelocity(null);
    }
}

