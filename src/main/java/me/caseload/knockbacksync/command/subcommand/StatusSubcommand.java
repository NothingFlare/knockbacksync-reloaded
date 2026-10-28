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
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class StatusSubcommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        ConfigManager configManager = KnockbackSync.getInstance().getConfigManager();

        // Global status
        boolean globalStatus = configManager.isToggled();
        sender.sendMessage(ChatColor.YELLOW + "Global KnockbackSync status: " +
                (globalStatus ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));

        Player target = null;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        }

        if (target == null) {
            return true;
        }

        boolean isSelf = sender.equals(target);
        if (isSelf || sender.hasPermission("knockbacksync.status.other") || sender.isOp()) {
            UUID uuid = target.getUniqueId();
            boolean isExempt = PlayerDataManager.shouldExempt(uuid);
            boolean playerStatus = PlayerDataManager.containsPlayerData(uuid);

            if (isExempt) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s status: " +
                        ChatColor.GRAY + "Exempt (Bedrock/Geyser)");
            } else if (!globalStatus) {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s status: " +
                        ChatColor.RED + "Disabled (Global toggle is off)");
            } else {
                sender.sendMessage(ChatColor.YELLOW + target.getName() + "'s status: " +
                        (playerStatus ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
            }
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission to check status for other players.");
        }
        return true;
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
