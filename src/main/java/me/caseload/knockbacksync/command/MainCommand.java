/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command;

import dev.jorel.commandapi.CommandAPICommand;
import me.caseload.knockbacksync.command.subcommand.PingSubcommand;
import me.caseload.knockbacksync.command.subcommand.ReloadSubcommand;
import me.caseload.knockbacksync.command.subcommand.StatusSubcommand;
import me.caseload.knockbacksync.command.subcommand.ToggleSubcommand;
import org.bukkit.ChatColor;

public class MainCommand {

    public void register() {
        new CommandAPICommand("knockbacksync")
                .withAliases("kbsync")
                .withSubcommand(new PingSubcommand().getCommand())
                .withSubcommand(new ToggleSubcommand().getCommand())
                .withSubcommand(new ReloadSubcommand().getCommand())
                .withSubcommand(new StatusSubcommand().getCommand())
                .executes((sender, args) -> {
                    sender.sendMessage(ChatColor.GOLD + "=== " + ChatColor.YELLOW + "KnockbackSync Reloaded v1.0" + ChatColor.GOLD + " ===");
                    sender.sendMessage(ChatColor.YELLOW + "/kbsync ping [player] " + ChatColor.GRAY + "- Check measured & compensated ping");
                    sender.sendMessage(ChatColor.YELLOW + "/kbsync status [player] " + ChatColor.GRAY + "- View global & player sync status");
                    sender.sendMessage(ChatColor.YELLOW + "/kbsync toggle [player] " + ChatColor.GRAY + "- Toggle globally or for a player");
                    sender.sendMessage(ChatColor.YELLOW + "/kbsync reload " + ChatColor.GRAY + "- Reload configuration");
                })
                .register();
    }
}

