/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command;

import me.caseload.knockbacksync.command.subcommand.PingSubcommand;
import me.caseload.knockbacksync.command.subcommand.ReloadSubcommand;
import me.caseload.knockbacksync.command.subcommand.StatusSubcommand;
import me.caseload.knockbacksync.command.subcommand.ToggleSubcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public MainCommand() {
        subCommands.put("ping", new PingSubcommand());
        subCommands.put("status", new StatusSubcommand());
        subCommands.put("toggle", new ToggleSubcommand());
        subCommands.put("reload", new ReloadSubcommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subName);
        if (subCommand != null) {
            return subCommand.execute(sender, args);
        }

        sendHelp(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            String prefix = args[0].toLowerCase();
            for (String sub : subCommands.keySet()) {
                if (hasPermissionForSub(sender, sub) && sub.startsWith(prefix)) {
                    list.add(sub);
                }
            }
            return list;
        } else if (args.length > 1) {
            String subName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subName);
            if (subCommand != null) {
                return subCommand.tabComplete(sender, args);
            }
        }
        return Collections.emptyList();
    }

    private boolean hasPermissionForSub(CommandSender sender, String sub) {
        if (sender.isOp()) return true;
        return switch (sub) {
            case "ping" -> sender.hasPermission("knockbacksync.ping");
            case "status" -> true;
            case "toggle" -> sender.hasPermission("knockbacksync.toggle.global")
                    || sender.hasPermission("knockbacksync.toggle.self")
                    || sender.hasPermission("knockbacksync.toggle.other");
            case "reload" -> sender.hasPermission("knockbacksync.reload");
            default -> false;
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "KnockbackSync Reloaded v1.0" + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.YELLOW + "/kbsync ping [player] " + ChatColor.GRAY + "- Check measured & compensated ping");
        sender.sendMessage(ChatColor.YELLOW + "/kbsync status [player] " + ChatColor.GRAY + "- View global & player sync status");
        sender.sendMessage(ChatColor.YELLOW + "/kbsync toggle [player] " + ChatColor.GRAY + "- Toggle globally or for a player");
        sender.sendMessage(ChatColor.YELLOW + "/kbsync reload " + ChatColor.GRAY + "- Reload configuration");
    }
}
