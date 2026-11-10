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
        if (entityDamageEvent == null) {
            return;
        }

        EntityDamageEvent.DamageCause cause = entityDamageEvent.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_ATTACK && cause != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
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

        if (!playerData.isPendingKnockback()) {
            return;
        }
        playerData.setPendingKnockback(false);

        Double verticalVelocity = playerData.getVerticalVelocity();
        // Always consume the calculated vertical velocity so it doesn't linger for unrelated velocity events
        playerData.setVerticalVelocity(null);

        if (verticalVelocity == null) {
            return;
        }

        Integer damageTicks = playerData.getLastDamageTicks();
        if (damageTicks != null && damageTicks > 8) {
            return;
        }

        Vector velocity = event.getVelocity();
        if (!playerData.isOnGround(velocity.getY())) {
            return;
        }

        Vector adjustedVelocity = velocity.clone().setY(verticalVelocity);
        event.setVelocity(adjustedVelocity);
    }
}

