package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.scoreboard.ScoreBoardHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathHandler implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        int oldDeaths = DeathStatsDAO.playerDeaths.getOrDefault(playerId, 0);
        int oldRank = getPlayerRank(playerId);
        String playerAbove = getPlayerAbove(oldRank);

        int newDeaths = oldDeaths + 1;
        DeathStatsDAO.playerDeaths.put(playerId, newDeaths);

        int newRank = getPlayerRank(playerId);
        String newPlayerAbove = getPlayerAbove(newRank);

        String deathWord = newDeaths == 1 ? "death" : "deaths";
        String message = ChatColor.YELLOW + player.getName() + ChatColor.WHITE + " now has " +
                ChatColor.RED + newDeaths + ChatColor.WHITE + " " + deathWord + ".";

        if (newRank < oldRank) {
            message += "\n" + ChatColor.GREEN + "They went up in the leaderboard from place " +
                    ChatColor.YELLOW + oldRank + ChatColor.GREEN + " (under " + ChatColor.YELLOW + playerAbove +
                    ChatColor.GREEN + ") to " + ChatColor.YELLOW + newRank;
            if (newRank == 1) {
                message += ChatColor.GREEN + " (on top)!";
            } else {
                message += ChatColor.GREEN + " (under " + ChatColor.YELLOW + newPlayerAbove + ChatColor.GREEN + ").";
            }
        } else if (newRank == oldRank) {
            if (oldDeaths == 0) {
                message += "\n" + ChatColor.YELLOW + "They've entered the leaderboard at place " + newRank;
            } else {
                message += "\n" + ChatColor.YELLOW + "They've increased their death count but stay at place " + newRank;
            }
            if (newRank > 1) {
                message += " (under " + ChatColor.YELLOW + newPlayerAbove + ChatColor.YELLOW + ").";
            } else {
                message += " (on top).";
            }
        } else {
            message += "\n" + ChatColor.RED + "They've dropped to place " + newRank + " on the leaderboard";
            if (newRank > 1) {
                message += " (under " + ChatColor.YELLOW + newPlayerAbove + ChatColor.RED + ").";
            }
        }

        Bukkit.broadcastMessage(message);

        DeathStatsDAO.saveDeathStats();

        if (DeathStatsDAO.scoreboardEnabled) {
            ScoreBoardHandler.updateScoreboard();
        }
    }


    private int getPlayerRank(UUID playerId) {
        List<Map.Entry<UUID, Integer>> sortedEntries = DeathStatsDAO.playerDeaths.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        for (int i = 0; i < sortedEntries.size(); i++) {
            if (sortedEntries.get(i).getKey().equals(playerId)) {
                return i + 1;
            }
        }
        return sortedEntries.size() + 1;
    }

    private String getPlayerAbove(int rank) {
        List<Map.Entry<UUID, Integer>> sortedEntries = DeathStatsDAO.playerDeaths.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();

        if (rank <= 1 || rank > sortedEntries.size()) return "None";

        UUID playerAboveId = sortedEntries.get(rank - 2).getKey();
        return Bukkit.getOfflinePlayer(playerAboveId).getName();
    }
}
