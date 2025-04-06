package org.minecraft.tsunami.deathStats.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

public class ReloadCommand {

    public static boolean handleReloadCommand(ConfigManager configManager, ScoreboardHandler scoreboardHandler, HealthDisplayManager healthDisplayManager, CommandSender sender) {
        if (!sender.hasPermission("deathstats.reload")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", ""));
            return true;
        }

        configManager.loadConfig();

        DeathStatsDAO.loadDeathStats();

        scoreboardHandler.reload();
        healthDisplayManager.reload();

        sender.sendMessage(configManager.getFormattedMessage("reload", "&aConfiguration reloaded. Changes applied to online players."));
        return true;
    }
}