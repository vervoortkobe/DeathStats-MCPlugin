package org.minecraft.tsunami.deathStats.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerUtil {

    public static int getPlayerRank(UUID playerId) {
        int deaths = DeathStatsDAO.getPlayerDeaths(playerId);

        long playersWithMoreDeaths = DeathStatsDAO.getAllPlayerDeaths().entrySet().stream()
                .filter(entry -> !entry.getKey().equals(playerId) && entry.getValue() > deaths)
                .count();

        return (int) playersWithMoreDeaths + 1;
    }

    public static List<Map.Entry<UUID, Integer>> getSortedDeathEntries() {
        return DeathStatsDAO.getAllPlayerDeaths().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    public static String getPlayerNameAboveRank(int rank, List<Map.Entry<UUID, Integer>> sortedEntries) {
        if (rank <= 1 || rank - 2 >= sortedEntries.size()) {
            return null;
        }
        UUID playerAboveId = sortedEntries.get(rank - 2).getKey();
        OfflinePlayer playerAbove = Bukkit.getOfflinePlayer(playerAboveId);
        return playerAbove.getName() != null ? playerAbove.getName() : "Unknown";
    }

    public static String getColorForRank(int rank) {
        return switch (rank) {
            case 1 -> ChatColor.GOLD.toString();
            case 2 -> ChatColor.YELLOW.toString();
            case 3 -> ChatColor.GRAY.toString();
            default -> ChatColor.WHITE.toString();
        };
    }

    public static String getHealthColor(double currentHealth, double maxHealth, ConfigManager configManager) {
        if (maxHealth <= 0) return configManager.getHealthColorLow();
        double percentage = (currentHealth / maxHealth) * 100.0;
        if (percentage >= 67) {
            return configManager.getHealthColorHigh();
        } else if (percentage >= 34) {
            return configManager.getHealthColorMedium();
        } else {
            return configManager.getHealthColorLow();
        }
    }
}