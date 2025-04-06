package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.util.PlayerUtil;

import java.util.UUID;

public class CheckCommand {

    public static boolean handleCheckCommand(ConfigManager configManager, DeathStatsDAO dao, CommandSender sender, String[] args) {

        OfflinePlayer targetPlayer;
        String targetNameArg = null; // Store the name provided in args if applicable

        if (args.length < 2) {
            // Check self
            if (!(sender instanceof Player player)) {
                sender.sendMessage(configManager.getFormattedMessage("player-only", "&cConsole usage: /ds check <player>"));
                return true;
            }
            if (!sender.hasPermission("deathstats.check") && !sender.hasPermission("deathstats.admin")) {
                sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
                return true;
            }
            targetPlayer = player;
        } else {
            // Check other
            if (!sender.hasPermission("deathstats.check.others") && !sender.hasPermission("deathstats.admin")) {
                sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
                return true;
            }
            targetNameArg = args[1];
            @SuppressWarnings("deprecation")
            OfflinePlayer lookedUpPlayer = Bukkit.getOfflinePlayer(targetNameArg);
            if (!lookedUpPlayer.hasPlayedBefore()) {
                sender.sendMessage(configManager.getFormattedMessage("invalid-player", "&cPlayer not found.", "player", targetNameArg));
                return true;
            }
            targetPlayer = lookedUpPlayer;
        }

        UUID targetUUID = targetPlayer.getUniqueId();
        // Prefer actual name, fallback to arg if name is null (can happen)
        String targetName = targetPlayer.getName() != null ? targetPlayer.getName() : (targetNameArg != null ? targetNameArg : "Unknown");

        int deaths = dao.getPlayerDeaths(targetUUID); // Use dao instance
        int rank = PlayerUtil.getPlayerRank(targetUUID);
        String rankColorStr = PlayerUtil.getColorForRank(rank);

        // Send messages using ConfigManager
        sender.sendMessage(configManager.getFormattedMessage("check-header", "&6--- Stats for %player% ---", "player", targetName));
        sender.sendMessage(configManager.getFormattedMessageNoPrefix("check-deaths", "&cDeaths: &f%deaths%", "deaths", String.valueOf(deaths)));
        sender.sendMessage(configManager.getFormattedMessageNoPrefix("check-rank", "&eRank: &f{rank_color}#%rank%", "rank", String.valueOf(rank), "rank_color", rankColorStr));

        return true;
    }
}