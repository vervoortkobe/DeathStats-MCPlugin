package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.util.PlayerUtil; // Use PlayerUtil

import java.util.UUID;

public class CheckCommand {

    public static boolean handleCheckCommand(ConfigManager configManager, CommandSender sender, String[] args) {

        OfflinePlayer targetPlayer;

        if (args.length < 2) {
            // Check self
            if (!(sender instanceof Player player)) {
                sender.sendMessage(configManager.getFormattedMessage("player-only", "&cConsole cannot check self. Usage: /ds check <player>"));
                return true;
            }
            if (!sender.hasPermission("deathstats.check")) {
                sender.sendMessage(configManager.getFormattedMessage("no-permission", ""));
                return true;
            }
            targetPlayer = player;
        } else {
            // Check other
            if (!sender.hasPermission("deathstats.check.others")) {
                sender.sendMessage(configManager.getFormattedMessage("no-permission", ""));
                return true;
            }
            // Use deprecated method for name lookup, common in commands
            @SuppressWarnings("deprecation")
            OfflinePlayer lookedUpPlayer = Bukkit.getOfflinePlayer(args[1]);
            if (lookedUpPlayer == null || !lookedUpPlayer.hasPlayedBefore()) {
                sender.sendMessage(configManager.getFormattedMessage("invalid-player", "", "player", args[1]));
                return true;
            }
            targetPlayer = lookedUpPlayer;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : args.length >= 2 ? args[1] : "Unknown";
        int deaths = DeathStatsDAO.getPlayerDeaths(targetUUID);
        int rank = PlayerUtil.getPlayerRank(targetUUID);
        String rankColorStr = PlayerUtil.getColorForRank(rank);

        // Send messages using ConfigManager
        sender.sendMessage(configManager.getFormattedMessage("check-header", "", "player", targetName));
        sender.sendMessage(configManager.getFormattedMessage("check-deaths", "", "deaths", String.valueOf(deaths)));
        sender.sendMessage(configManager.getFormattedMessage("check-rank", "", "rank", String.valueOf(rank), "rank_color", rankColorStr));

        return true;
    }
}