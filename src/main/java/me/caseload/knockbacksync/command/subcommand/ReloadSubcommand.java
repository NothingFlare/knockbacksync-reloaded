/*
 * Copyright (C) 2026 Axionize
 * Copyright (C) 2026 NothingFlare (Modifications)
 *
 * Licensed under GNU GPLv3.
 */

package me.caseload.knockbacksync.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import me.caseload.knockbacksync.KnockbackSync;
import me.caseload.knockbacksync.manager.ConfigManager;
import org.bukkit.ChatColor;

public class ReloadSubcommand {

    public CommandAPICommand getCommand() {
        return new CommandAPICommand("reload")
                .withPermission("knockbacksync.reload")
                .executes((sender, args) -> {
                    ConfigManager configManager = KnockbackSync.getInstance().getConfigManager();
                    configManager.loadConfig(true);

                    String reloadMessage = ChatColor.translateAlternateColorCodes(
                            '&',
                            configManager.getReloadMessage()
                    );
                    sender.sendMessage(reloadMessage);
                });
    }
}

