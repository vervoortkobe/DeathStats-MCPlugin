package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.util.PlayerUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TopCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleTopCommand(ConfigManager configManager, CommandSender sender) {
        if (!sender.hasPermission("deathstats.top") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        List<Map.Entry<UUID, Integer>> sortedEntries = PlayerUtil.getSortedDeathEntries();

        if (sortedEntries.isEmpty()) {
            sender.sendMessage(configManager.getFormattedMessage("top-no-stats", "&eNo death statistics available yet."));
            return true;
        }

        int limit = 10;
        sender.sendMessage(configManager.getFormattedMessage("top-header", "&6--- Deaths Leaderboard (Top %limit%) ---", "limit", String.valueOf(limit)));

        for (int i = 0; i < limit && i < sortedEntries.size(); i++) {
            Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
            UUID playerId = entry.getKey();
            int deaths = entry.getValue();
            int rank = i + 1;

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            String rankColorStr = PlayerUtil.getColorForRank(rank);

            String format = configManager.getScoreboardEntryFormat();
            String displayText = format
                    .replace("{rank}", String.valueOf(rank))
                    .replace("{rank_color}", rankColorStr)
                    .replace("{name}", name)
                    .replace("{deaths}", String.valueOf(deaths));

            sender.sendMessage(configManager.getPrefix() + displayText);
        }

        return true;
    }
}