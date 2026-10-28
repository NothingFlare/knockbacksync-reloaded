/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import me.caseload.knockbacksync.manager.PlayerData;
import me.caseload.knockbacksync.manager.PlayerDataManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PingSubcommand {

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("ping")
                .withPermission("knockbacksync.ping")
                .withOptionalArguments(new PlayerArgument("target"))
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null && sender instanceof Player) {
                        target = (Player) sender;
                    }

                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Usage: /knockbacksync ping <player>");
                        return;
                    }

                    if (PlayerDataManager.shouldExempt(target.getUniqueId())) {
                        sender.sendMessage(ChatColor.YELLOW + target.getName() + " is exempt from KnockbackSync (Bedrock/Geyser).");
                        return;
                    }

                    PlayerData playerData = PlayerDataManager.getPlayerData(target.getUniqueId());
                    if (playerData == null) {
                        sender.sendMessage(ChatColor.YELLOW + target.getName() + " currently has KnockbackSync disabled.");
                        return;
                    }

                    long ping = playerData.getSmoothedPing();
                    long effective = playerData.getEffectivePing();
                    sender.sendMessage(ChatColor.GOLD + target.getName() + "'s " +
                            ChatColor.YELLOW + "measured ping: " + ChatColor.GREEN + ping + "ms " +
                            ChatColor.GRAY + "(effective compensation: " + ChatColor.AQUA + effective + "ms" + ChatColor.GRAY + ").");
                });
    }
}

