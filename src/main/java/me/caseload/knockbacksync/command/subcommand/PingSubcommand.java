/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command.subcommand;

import me.caseload.knockbacksync.command.SubCommand;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingSubcommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knockbacksync.ping") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to check player ping.");
            return true;
        }

        Player target = null;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' is not online.");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /knockbacksync ping <player>");
            return true;
        }

        if (PlayerDataManager.shouldExempt(target.getUniqueId())) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " is exempt from KnockbackSync (Bedrock/Geyser).");
            return true;
        }

        PlayerData playerData = PlayerDataManager.getPlayerData(target.getUniqueId());
        if (playerData == null) {
            sender.sendMessage(ChatColor.YELLOW + target.getName() + " currently has KnockbackSync disabled.");
            return true;
        }

        long ping = playerData.getSmoothedPing();
        long effective = playerData.getEffectivePing();
        sender.sendMessage(ChatColor.GOLD + target.getName() + "'s " +
                ChatColor.YELLOW + "measured ping: " + ChatColor.GREEN + ping + "ms " +
                ChatColor.GRAY + "(effective compensation: " + ChatColor.AQUA + effective + "ms" + ChatColor.GRAY + ").");
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
