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
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class ReloadSubcommand implements SubCommand {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("knockbacksync.reload") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to reload KnockbackSync.");
            return true;
        }

        ConfigManager configManager = KnockbackSync.getInstance().getConfigManager();
        configManager.loadConfig(true);

        String reloadMessage = ChatColor.translateAlternateColorCodes(
                '&',
                configManager.getReloadMessage()
        );
        sender.sendMessage(reloadMessage);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
