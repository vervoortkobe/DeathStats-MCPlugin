package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.Bukkit;
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

    public PlayerDeathHandler(ConfigManager configManager, ScoreboardHandler scoreboardHandler) {
        this.configManager = configManager;
        this.scoreboardHandler = scoreboardHandler;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();

        int oldDeaths = DeathStatsDAO.getPlayerDeaths(playerId);
        List<Map.Entry<UUID, Integer>> sortedListBefore = PlayerUtil.getSortedDeathEntries();
        int oldRank = PlayerUtil.getPlayerRank(playerId);
        String playerAboveOldName = PlayerUtil.getPlayerNameAboveRank(oldRank, sortedListBefore);
        if (playerAboveOldName == null) configManager.getRawMessage("rank-player-above-none", "Top");

        DeathStatsDAO.incrementPlayerDeaths(playerId);

        int newDeaths = DeathStatsDAO.getPlayerDeaths(playerId);
        List<Map.Entry<UUID, Integer>> sortedListAfter = PlayerUtil.getSortedDeathEntries();
        int newRank = PlayerUtil.getPlayerRank(playerId);
        String playerAboveNewName = PlayerUtil.getPlayerNameAboveRank(newRank, sortedListAfter);
        if (playerAboveNewName == null) playerAboveNewName = configManager.getRawMessage("rank-player-above-none", "Top");

        String rankChangeInfo = "";
        if (oldDeaths == 0 && newDeaths > 0) {
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-new-entry-message", "&e(New Entry)");
        } else if (newRank < oldRank) {
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-up-message", "&a↑{old_rank} to {new_rank} (above {player_above_new})",
                    "old_rank", String.valueOf(oldRank),
                    "new_rank", String.valueOf(newRank),
                    "player_above_new", playerAboveNewName);
        } else if (newRank > oldRank) {
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-down-message", "&c↓{old_rank} to {new_rank} (below {player_above_new})",
                    "old_rank", String.valueOf(oldRank),
                    "new_rank", String.valueOf(newRank),
                    "player_above_new", playerAboveNewName);
        } else {
            rankChangeInfo = configManager.getFormattedMessageNoPrefix("rank-same-message", "");
        }

        String rankColorStr = PlayerUtil.getColorForRank(newRank);
        String broadcastMessage = configManager.getFormattedMessageNoPrefix("death-broadcast",
                "{prefix}&e{player} &fnow has &c{deaths} &fdeaths. Rank: {rank_color}#%rank% {rank_change_info}",
                "prefix", configManager.getPrefix(),
                "player", player.getName(),
                "deaths", String.valueOf(newDeaths),
                "rank", String.valueOf(newRank),
                "rank_color", rankColorStr,
                "rank_change_info", rankChangeInfo.trim());

        Bukkit.broadcastMessage(broadcastMessage);

        scoreboardHandler.updateScoreboard();
    }
}