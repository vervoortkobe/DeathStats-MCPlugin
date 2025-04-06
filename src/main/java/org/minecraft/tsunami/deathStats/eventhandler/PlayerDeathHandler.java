// src/main/java/.../eventhandler/PlayerDeathHandler.java
package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;
import org.minecraft.tsunami.deathStats.util.PlayerUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerDeathHandler implements Listener {

    private final ConfigManager configManager;
    private final ScoreboardHandler scoreboardHandler;
    // HealthDisplayManager might be needed if you want immediate updates on death,
    // but the respawn event might be better for tab list.

    public PlayerDeathHandler(ConfigManager configManager, ScoreboardHandler scoreboardHandler) {
        this.configManager = configManager;
        this.scoreboardHandler = scoreboardHandler;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        // Get data BEFORE incrementing deaths
        int oldDeaths = DeathStatsDAO.getPlayerDeaths(playerId);
        List<Map.Entry<UUID, Integer>> sortedListBefore = PlayerUtil.getSortedDeathEntries();
        int oldRank = PlayerUtil.getPlayerRank(playerId); // Rank before death
        String playerAboveOldName = PlayerUtil.getPlayerNameAboveRank(oldRank, sortedListBefore);
        if (playerAboveOldName == null) playerAboveOldName = configManager.getRawMessage("rank-player-above-none", "Top");


        // Increment and Save Death
        DeathStatsDAO.incrementPlayerDeaths(playerId); // This now saves internally

        // Get data AFTER incrementing deaths
        int newDeaths = DeathStatsDAO.getPlayerDeaths(playerId); // Should be oldDeaths + 1
        List<Map.Entry<UUID, Integer>> sortedListAfter = PlayerUtil.getSortedDeathEntries(); // Re-fetch sorted list after update
        int newRank = PlayerUtil.getPlayerRank(playerId); // Rank after death
        String playerAboveNewName = PlayerUtil.getPlayerNameAboveRank(newRank, sortedListAfter);
        if (playerAboveNewName == null) playerAboveNewName = configManager.getRawMessage("rank-player-above-none", "Top");


        // --- Broadcast Logic ---
        String rankChangeInfo = "";
        if (oldDeaths == 0 && newDeaths > 0) { // First death recorded
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-new-entry-message", "&e(New Entry)");
        } else if (newRank < oldRank) { // Rank Up
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-up-message", "&a↑{old_rank} to {new_rank} (above {player_above_new})",
                    "old_rank", String.valueOf(oldRank),
                    "new_rank", String.valueOf(newRank),
                    "player_above_new", playerAboveNewName);
        } else if (newRank > oldRank) { // Rank Down
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-down-message", "&c↓{old_rank} to {new_rank} (below {player_above_new})",
                    "old_rank", String.valueOf(oldRank),
                    "new_rank", String.valueOf(newRank),
                    "player_above_new", playerAboveNewName);
        } else { // Rank Same
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-same-message", ""); // Default empty if no change msg needed
        }

        String rankColorStr = PlayerUtil.getColorForRank(newRank);
        String broadcastMessage = configManager.getFormattedMessageNoPrefix("death-broadcast", // Use non-prefixed version
                "{prefix}&e{player} &fnow has &c{deaths} &fdeaths. Rank: {rank_color}#%rank% {rank_change_info}",
                "prefix", configManager.getPrefix(), // Manually add prefix placeholder if needed in message
                "player", player.getName(),
                "deaths", String.valueOf(newDeaths),
                "rank", String.valueOf(newRank),
                "rank_color", rankColorStr, // Pass color code string
                "rank_change_info", rankChangeInfo.trim()); // Pass the generated change info

        Bukkit.broadcastMessage(broadcastMessage);

        // Update Scoreboard
        scoreboardHandler.updateScoreboard(); // Update immediately after death
    }
}