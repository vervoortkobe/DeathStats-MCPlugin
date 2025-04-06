package org.minecraft.tsunami.deathStats.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.Main; // Need Main instance
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardManager;

public class ReloadCommand {

    // Pass all necessary components to the handler
    public static boolean handleReloadCommand(Main plugin, ConfigManager configManager, DeathStatsDAO dao, ScoreboardManager scoreboardManager, HealthDisplayManager healthDisplayManager, CommandSender sender) {
        if (!sender.hasPermission("deathstats.reload") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        // 1. Reload config.yml
        configManager.loadConfig();

        // 2. Reload deaths.yml (optional)
        // dao.loadDeathStats(); // Uncomment if manual edits to deaths.yml should be loaded

        // 3. Trigger reload logic in managers
        scoreboardManager.reload();
        healthDisplayManager.reload();

        sender.sendMessage(configManager.getFormattedMessage("reload", "&aConfiguration reloaded."));
        return true;
    }
}