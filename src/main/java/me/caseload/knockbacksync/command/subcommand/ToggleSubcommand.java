/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command.subcommand;

import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.command.SubCommand;
import me.caseload.knockbacksync.manager.ConfigManager;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ToggleSubcommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        ConfigManager configManager = KnockbackSync.getInstance().getConfigManager();

        if (args.length <= 1) {
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
            return true;
        }

        // Player-specific toggle
        if (!configManager.isToggled()) {
            sender.sendMessage(ChatColor.RED + "KnockbackSync is currently globally disabled on this server.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
            return true;
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
        return true;
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

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> list = new ArrayList<>();
            String prefix = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    list.add(player.getName());
                }
            }
            return list;
        }
        return Collections.emptyList();
    }
}
