package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.ScoreboardManager;

import java.util.UUID;

public class ResetCommand {

    public static boolean handleResetCommand(ConfigManager configManager, DeathStatsDAO dao, ScoreboardManager scoreboardManager, CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathstats.reset") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getFormattedMessage("reset-usage", "&cUsage: /ds reset <player>"));
            return true;
        }

        String targetName = args[1];
        @SuppressWarnings("deprecation")
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(configManager.getFormattedMessage("invalid-player", "&cPlayer not found.", "player", targetName));
            return true;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        String actualName = targetPlayer.getName() != null ? targetPlayer.getName() : targetName;

        dao.resetPlayerDeaths(targetUUID); // Use DAO method (saves internally)
        scoreboardManager.updateScoreboard(); // Update scoreboard after change

        sender.sendMessage(configManager.getFormattedMessage("reset-success", "&aDeaths reset.", "player", actualName));
        return true;
    }
}