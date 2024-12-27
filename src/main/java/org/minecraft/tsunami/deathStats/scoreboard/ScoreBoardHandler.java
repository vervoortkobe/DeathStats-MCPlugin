package org.minecraft.tsunami.deathStats.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.minecraft.tsunami.deathStats.util.MainUtil.getColorForRank;

public class ScoreBoardHandler {

    public static Scoreboard scoreboard;
    public static Objective deathObjective;

    public static void setupScoreboard() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        assert manager != null;
        scoreboard = manager.getNewScoreboard();
        deathObjective = scoreboard.registerNewObjective("Deaths", "dummy",
                ChatColor.BOLD + "" + ChatColor.RED + "☠ Top Deaths ☠");
        deathObjective.setDisplaySlot(DisplaySlot.SIDEBAR);

        updateScoreboard();
    }

    public static void updateScoreboard() {
        if (!DeathStatsDAO.scoreboardEnabled) return;

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<Map.Entry<UUID, Integer>> sortedEntries = DeathStatsDAO.playerDeaths.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(5)
                .toList();

        for (int i = 0; i < sortedEntries.size(); i++) {
            Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
            UUID playerId = entry.getKey();
            String name = Bukkit.getOfflinePlayer(playerId).getName();
            int deaths = entry.getValue();

            ChatColor rankColor = getColorForRank(i + 1);

            String displayText = String.format("%s%d. %s%s %s(%s%d%s)",
                    rankColor, i + 1,
                    rankColor, name,
                    ChatColor.WHITE, ChatColor.BOLD, deaths, ChatColor.RESET + "" + ChatColor.GRAY);

            Score score = deathObjective.getScore(displayText);
            score.setScore(5 - i);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }
}
