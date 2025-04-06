package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

import java.util.UUID;

public class SetCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleSetCommand(ConfigManager configManager, DeathStatsDAO dao, ScoreboardHandler scoreboardHandler, CommandSender sender, String[] args) {
        if (!sender.hasPermission("deathstats.set") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getFormattedMessage("set-usage", "&cUsage: /ds set <player> <amount>"));
            return true;
        }

        String targetName = args[1];
        @SuppressWarnings("deprecation")
        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(targetName);
        if (!targetPlayer.hasPlayedBefore()) {
            sender.sendMessage(configManager.getFormattedMessage("invalid-player", "&cPlayer not found.", "player", targetName));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) {
                sender.sendMessage(configManager.getFormattedMessage("number-must-be-positive", "&cNumber must be zero or positive."));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(configManager.getFormattedMessage("invalid-number", "&cInvalid number.", "input", args[2]));
            return true;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        String actualName = targetPlayer.getName() != null ? targetPlayer.getName() : targetName;

        dao.setPlayerDeaths(targetUUID, amount); // Use DAO method (saves internally)
        scoreboardHandler.updateScoreboard(); // Update scoreboard after change

        sender.sendMessage(configManager.getFormattedMessage("set-success", "&aDeaths set.", "player", actualName, "deaths", String.valueOf(amount)));
        return true;
    }
}