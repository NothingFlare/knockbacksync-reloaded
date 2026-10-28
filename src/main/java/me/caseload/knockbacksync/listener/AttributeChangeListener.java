/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.attribute.Attributes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import me.caseload.knockbacksync.util.MathUtil;
import org.bukkit.entity.Player;

import java.util.List;

public class AttributeChangeListener extends PacketListenerAbstract {

    private static final double MIN_GRAVITY = -1.0;
    private static final double MAX_GRAVITY = 1.0;
    private static final double DEFAULT_GRAVITY = 0.08;

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.UPDATE_ATTRIBUTES) {
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

        WrapperPlayServerUpdateAttributes packet = new WrapperPlayServerUpdateAttributes(event);

        for (WrapperPlayServerUpdateAttributes.Property property : packet.getProperties()) {
            if (property.getAttribute().equals(Attributes.GENERIC_GRAVITY)) {
                double computedGravity = calculateValueWithModifiers(property);
                if (playerData.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_5)) {
                    playerData.setGravity(computedGravity);
                } else {
                    playerData.setGravity(DEFAULT_GRAVITY);
                }
                break;
            }
        }
    }

    private double calculateValueWithModifiers(WrapperPlayServerUpdateAttributes.Property property) {
        double baseValue = property.getValue();
        double additionSum = 0.0;
        double multiplyBaseSum = 0.0;
        double multiplyTotalProduct = 1.0;

        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = property.getModifiers();
        if (modifiers != null) {
            for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : modifiers) {
                switch (modifier.getOperation()) {
                    case ADDITION:
                        additionSum += modifier.getAmount();
                        break;
                    case MULTIPLY_BASE:
                        multiplyBaseSum += modifier.getAmount();
                        break;
                    case MULTIPLY_TOTAL:
                        multiplyTotalProduct *= (1.0 + modifier.getAmount());
                        break;
                }
            }
        }

        double newValue = (baseValue + additionSum) * (1.0 + multiplyBaseSum) * multiplyTotalProduct;
        return MathUtil.clamp(newValue, MIN_GRAVITY, MAX_GRAVITY);
    }
}

