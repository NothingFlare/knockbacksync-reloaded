/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPong;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.entity.Player;

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
        Long sendTime = playerData.getPendingPings().remove(packetId);
        if (sendTime == null) {
            return;
        }

        long rawPing = System.currentTimeMillis() - sendTime;
        playerData.recordPingSample(rawPing);
    }
}

