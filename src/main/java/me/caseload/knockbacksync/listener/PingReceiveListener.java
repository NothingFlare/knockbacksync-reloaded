/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.manager.CombatManager;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PingReceiveListener extends PacketListenerAbstract {

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!KnockbackSync.getInstance().getConfigManager().isToggled()) {
            return;
        }

        if (event.getPacketType() != PacketType.Play.Client.PONG) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        PlayerData playerData = PlayerDataManager.getPlayerData(player.getUniqueId());
        if (playerData == null) {
            return;
        }

        int packetId = new WrapperPlayClientPong(event).getId();
        long rawPing = playerData.removeInFlightPing(packetId);
        if (rawPing >= 0) {
            playerData.recordPingSample(rawPing);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        User user = event.getUser();
        if (user == null) {
            return;
        }
        UUID uuid = user.getUUID();
        if (uuid == null) {
            return;
        }

        CombatManager.removePlayer(uuid);
        PlayerData playerData = PlayerDataManager.removePlayerData(uuid);
        if (playerData != null) {
            playerData.quitCombat();
        }
    }
}
