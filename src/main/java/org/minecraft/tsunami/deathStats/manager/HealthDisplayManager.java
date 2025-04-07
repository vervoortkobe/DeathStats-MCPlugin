package org.minecraft.tsunami.deathStats.manager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.util.PlayerUtil;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class HealthDisplayManager {

    private final Main plugin;
    private final ConfigManager configManager;
    private final String belowNameObjectiveName = "ds_health_belowname";
    private BukkitTask tabUpdateTask;
    private final Set<UUID> playersWithTabHealth = new HashSet<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Set<UUID> playersWithBelowNameHealth = new HashSet<>();

    public HealthDisplayManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupBelowNameObjective();
        startTabUpdateTask();
    }

    private void setupBelowNameObjective() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard mainBoard = manager.getMainScoreboard();

        Objective objective = mainBoard.getObjective(belowNameObjectiveName);
        if (objective == null) {
            try {
                objective = mainBoard.registerNewObjective(belowNameObjectiveName, Criteria.HEALTH, ChatColor.RED + "❤");
                plugin.getLogger().info("Registered below-name health objective.");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not register below-name health objective: " + e.getMessage());
                return;
            }
        }
        if (configManager.isBelowNameHealthEnabled()) {
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        } else {
            if (objective.getDisplaySlot() == DisplaySlot.BELOW_NAME) {
                mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            }
        }
    }

    public void enableBelowNameHealth(Player player) {
        Scoreboard mainBoard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Objective objective = mainBoard.getObjective(belowNameObjectiveName);
        if (objective != null) {
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            playersWithBelowNameHealth.add(player.getUniqueId());
            plugin.getLogger().fine("Enabled below-name health display globally.");
        } else {
            plugin.getLogger().warning("Attempted to enable below-name health, but objective is missing.");
            setupBelowNameObjective();
            objective = mainBoard.getObjective(belowNameObjectiveName);
            if(objective != null) objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    public void disableBelowNameHealth(Player player) {
        Scoreboard mainBoard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Objective objective = mainBoard.getObjective(DisplaySlot.BELOW_NAME);
        if (objective != null && objective.getName().equals(belowNameObjectiveName)) {
            mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            plugin.getLogger().fine("Disabled below-name health display globally.");
        }
        playersWithBelowNameHealth.remove(player.getUniqueId());
    }

    public void applyBelowNameState(Player player) {
        if (configManager.isBelowNameHealthEnabled()) {
            enableBelowNameHealth(player);
        } else {
            disableBelowNameHealth(player);
        }
    }

    private void startTabUpdateTask() {
        stopTabUpdateTask();
        int intervalSeconds = configManager.getTabHealthUpdateIntervalSeconds();

        if (configManager.isTabHealthEnabled() && intervalSeconds > 0) {
            long intervalTicks = intervalSeconds * 20L;
            tabUpdateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    for (UUID uuid : new HashSet<>(playersWithTabHealth)) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            updateTabHealth(p);
                        } else {
                            playersWithTabHealth.remove(uuid);
                        }
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 20L, intervalTicks);
            plugin.getLogger().info("Tab list health update task started (interval: " + intervalSeconds + "s).");
        }
    }

    private void stopTabUpdateTask() {
        if (tabUpdateTask != null && !tabUpdateTask.isCancelled()) {
            tabUpdateTask.cancel();
            tabUpdateTask = null;
            plugin.getLogger().info("Tab list health update task stopped.");
        }
    }

    public void enableTabHealth(Player player) {
        playersWithTabHealth.add(player.getUniqueId());
        updateTabHealth(player);
        if (configManager.isTabHealthEnabled() && (tabUpdateTask == null || tabUpdateTask.isCancelled())) {
            startTabUpdateTask();
        }
    }

    public void disableTabHealth(Player player) {
        playersWithTabHealth.remove(player.getUniqueId());
        player.setPlayerListName(null);
    }

    public void updateTabHealth(Player player) {
        if (!player.isOnline() || !playersWithTabHealth.contains(player.getUniqueId())) {
            if (!playersWithTabHealth.contains(player.getUniqueId())) {
                player.getPlayerListName();
                if (player.getPlayerListName().contains("❤")) {
                    player.setPlayerListName(null);
                }
            }
            return;
        }
        if (!configManager.isTabHealthEnabled()) {
            player.setPlayerListName(null);
            return;
        }


        double currentHealth = player.getHealth();
        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
        String healthColor = PlayerUtil.getHealthColor(currentHealth, maxHealth, configManager);
        String format = configManager.getTabHealthFormat();

        String formattedName = format
                .replace("{name}", player.getName())
                .replace("{health}", String.format("%.1f", currentHealth))
                .replace("{max_health}", String.format("%.1f", maxHealth))
                .replace("{health_color}", healthColor);

        formattedName = ChatColor.translateAlternateColorCodes('&', formattedName);

        try {
            player.setPlayerListName(formattedName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error setting player list name for " + player.getName(), e);
        }
    }

    public void applyTabHealthState(Player player) {
        if (configManager.isTabHealthEnabled()) {
            enableTabHealth(player);
        } else {
            disableTabHealth(player);
        }
    }

    public void reload() {
        stopTabUpdateTask();
        if (configManager.isTabHealthEnabled()) {
            startTabUpdateTask();
            playersWithTabHealth.clear();
            for (Player p : Bukkit.getOnlinePlayers()) {
                enableTabHealth(p);
            }
        } else {
            for (UUID uuid : new HashSet<>(playersWithTabHealth)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) disableTabHealth(p);
            }
            playersWithTabHealth.clear();
        }

        setupBelowNameObjective();
        for(Player p : Bukkit.getOnlinePlayers()) {
            applyBelowNameState(p);
        }
    }

    public void cleanup() {
        stopTabUpdateTask();
        for (UUID uuid : playersWithTabHealth) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setPlayerListName(null);
        }
        playersWithTabHealth.clear();
        playersWithBelowNameHealth.clear();

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard mainBoard = manager.getMainScoreboard();
            Objective objective = mainBoard.getObjective(DisplaySlot.BELOW_NAME);
            if (objective != null && objective.getName().equals(belowNameObjectiveName)) {
                mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            }
        }
    }

}