package org.minecraft.tsunami.deathStats.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.util.PlayerUtil;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScoreboardHandler {

    private final Main plugin;
    private final ConfigManager configManager;
    private Scoreboard scoreboard;
    private Objective deathObjective;
    private BukkitTask updateTask;

    public ScoreboardHandler(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void setupScoreboard() {
        if (!configManager.isScoreboardEnabled()) {
            plugin.getLogger().info("Scoreboard is disabled in config, skipping setup.");
            return;
        }

        ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        if (bukkitManager == null) {
            plugin.getLogger().severe("Bukkit Scoreboard Manager is null! Cannot create scoreboard.");
            return;
        }

        scoreboard = bukkitManager.getNewScoreboard();
        try {
            deathObjective = scoreboard.registerNewObjective("ds_deaths", "dummy", configManager.getScoreboardTitle());
            deathObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            plugin.getLogger().info("DeathStats scoreboard initialized.");
            startUpdateTask();
            updateScoreboard();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Could not register scoreboard objective 'ds_deaths'. It might already exist or name is invalid: " + e.getMessage());
            deathObjective = scoreboard.getObjective("ds_deaths");
            if(deathObjective == null) {
                plugin.getLogger().severe("Failed to get or register scoreboard objective.");
                scoreboard = null;
                return;
            }
            deathObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }


    }

    public void updateScoreboard() {
        if (scoreboard == null || deathObjective == null || !configManager.isScoreboardEnabled()) {
            return;
        }

        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        List<Map.Entry<UUID, Integer>> sortedEntries = PlayerUtil.getSortedDeathEntries();
        int limit = configManager.getScoreboardEntries();
        String format = configManager.getScoreboardEntryFormat();

        for (int i = 0; i < limit && i < sortedEntries.size(); i++) {
            Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
            UUID playerId = entry.getKey();
            int deaths = entry.getValue();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            int rank = i + 1;
            String rankColorStr = PlayerUtil.getColorForRank(rank);

            String displayText = format
                    .replace("{rank}", String.valueOf(rank))
                    .replace("{rank_color}", rankColorStr)
                    .replace("{name}", name)
                    .replace("{deaths}", String.valueOf(deaths));

            displayText = ChatColor.translateAlternateColorCodes('&', displayText);

            if (displayText.length() > 40) {
                displayText = displayText.substring(0, 40);
            }

            try {
                Score score = deathObjective.getScore(displayText);
                score.setScore(limit - i);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Failed to set score for entry: '" + displayText + "'. Length: " + displayText.length() + ". Error: " + e.getMessage());
            }

        }

        applyToOnlinePlayers();
    }

    public void applyScoreboard(Player player) {
        if (scoreboard != null && configManager.isScoreboardEnabled()) {
            player.setScoreboard(scoreboard);
        } else {
            if (player.getScoreboard() == scoreboard) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    public void applyToOnlinePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyScoreboard(player);
        }
    }

    public void removeScoreboard() {
        stopUpdateTask();
        if (scoreboard != null) {
            ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
            if (bukkitManager != null) {
                Scoreboard mainScoreboard = bukkitManager.getMainScoreboard();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getScoreboard().equals(scoreboard)) {
                        player.setScoreboard(mainScoreboard);
                    }
                }
            }
            if (deathObjective != null) {
                try {
                    deathObjective.unregister();
                } catch (IllegalStateException ignored) { }
                deathObjective = null;
            }
            scoreboard = null;
            plugin.getLogger().info("DeathStats scoreboard removed.");
        }

    }

    private void startUpdateTask() {
        stopUpdateTask();
        int intervalSeconds = configManager.getScoreboardUpdateIntervalSeconds();
        if (intervalSeconds > 0) {
            long intervalTicks = intervalSeconds * 20L;
            updateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    updateScoreboard();
                }
            }.runTaskTimer(plugin, intervalTicks, intervalTicks);
            plugin.getLogger().info("Scoreboard update task started (interval: " + intervalSeconds + "s).");
        }
    }

    private void stopUpdateTask() {
        if (updateTask != null && !updateTask.isCancelled()) {
            updateTask.cancel();
            updateTask = null;
            plugin.getLogger().info("Scoreboard update task stopped.");
        }
    }

    public void reload() {
        if (configManager.isScoreboardEnabled()) {
            if (scoreboard == null || deathObjective == null) {
                setupScoreboard();
            } else {
                stopUpdateTask();
                startUpdateTask();
                updateScoreboard();
            }
            applyToOnlinePlayers();
        } else {
            removeScoreboard();
        }
    }

    public void cleanup() {
        stopUpdateTask();
        scoreboard = null;
        deathObjective = null;
    }

}