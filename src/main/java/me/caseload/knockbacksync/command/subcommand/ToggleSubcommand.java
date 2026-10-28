/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.manager.ConfigManager;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ToggleSubcommand {

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("toggle")
                .withPermission(CommandPermission.NONE)
                .withOptionalArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    ConfigManager configManager = KnockbackSync.getInstance().getConfigManager();
                    Player target = (Player) args.get("target");

                    if (target == null) {
                        // Global toggle
                        if (sender.hasPermission("knockbacksync.toggle.global") || sender.isOp()) {
                            boolean toggledState = !configManager.isToggled();
                            configManager.setToggled(toggledState);

                            KnockbackSync.getInstance().getConfig().set("enabled", toggledState);
                            KnockbackSync.getInstance().saveConfig();

                            String message = ChatColor.translateAlternateColorCodes(
                                    '&',
                                    toggledState ? configManager.getEnableMessage() : configManager.getDisableMessage()
                            );
                            sender.sendMessage(message);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to toggle the global setting.");
                        }
                    } else {
                        // Player-specific toggle
                        if (!configManager.isToggled()) {
                            sender.sendMessage(ChatColor.RED + "KnockbackSync is currently globally disabled on this server.");
                            return;
                        }

                        boolean isSelf = sender instanceof Player && sender.equals(target);
                        if (isSelf && (sender.hasPermission("knockbacksync.toggle.self") || sender.isOp())) {
                            togglePlayerKnockback(target, configManager, sender);
                        } else if (!isSelf && (sender.hasPermission("knockbacksync.toggle.other") || sender.isOp())) {
                            togglePlayerKnockback(target, configManager, sender);
                        } else {
                            sender.sendMessage(ChatColor.RED + "You don't have permission to toggle knockback for " +
                                    (isSelf ? "yourself." : "other players."));
                        }
                    }
                });
    }

    private void togglePlayerKnockback(Player target, ConfigManager configManager, CommandSender sender) {
        UUID uuid = target.getUniqueId();

        if (PlayerDataManager.shouldExempt(uuid)) {
            String message = ChatColor.translateAlternateColorCodes('&',
                    configManager.getPlayerIneligibleMessage()
            ).replace("%player%", target.getName());
            sender.sendMessage(message);
            return;
        }

        boolean hasPlayerData = PlayerDataManager.containsPlayerData(uuid);
        if (hasPlayerData) {
            PlayerData data = PlayerDataManager.removePlayerData(uuid);
            if (data != null) {
                data.quitCombat(true);
            }
        } else {
            PlayerDataManager.addPlayerData(uuid, new PlayerData(target));
        }

        String rawMsg = hasPlayerData ? configManager.getPlayerDisableMessage() : configManager.getPlayerEnableMessage();
        String message = ChatColor.translateAlternateColorCodes('&', rawMsg).replace("%player%", target.getName());
        sender.sendMessage(message);
    }
}

