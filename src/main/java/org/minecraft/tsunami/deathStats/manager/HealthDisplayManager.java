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
import org.minecraft.tsunami.deathStats.util.PlayerUtil; // Import PlayerUtil

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class HealthDisplayManager {

    private final Main plugin;
    private final ConfigManager configManager;
    private final String belowNameObjectiveName = "ds_health_belowname"; // Unique internal name
    private BukkitTask tabUpdateTask;
    private final Set<UUID> playersWithTabHealth = new HashSet<>(); // Track who has tab health ON
    private final Set<UUID> playersWithBelowNameHealth = new HashSet<>(); // Track who has below name ON

    public HealthDisplayManager(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupBelowNameObjective(); // Ensure objective exists if needed later
        startTabUpdateTask(); // Start tab list update task if enabled
    }

    // --- Below Name Health ---

    private void setupBelowNameObjective() {
        // We create the objective even if disabled by default,
        // so enabling it later doesn't require a reload.
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard mainBoard = manager.getMainScoreboard(); // Modify main board for global effect

        Objective objective = mainBoard.getObjective(belowNameObjectiveName);
        if (objective == null) {
            try {
                objective = mainBoard.registerNewObjective(belowNameObjectiveName, Criteria.HEALTH, ChatColor.RED + "❤"); // Use Criteria.HEALTH
                plugin.getLogger().info("Registered below-name health objective.");
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Could not register below-name health objective: " + e.getMessage());
                return; // Don't proceed if registration fails
            }
        }
        // Set display slot only if enabled in config *initially*
        if (configManager.isBelowNameHealthEnabled()) {
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        } else {
            // Ensure it's removed if initially disabled
            if (objective.getDisplaySlot() == DisplaySlot.BELOW_NAME) {
                mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            }
        }
    }

    public void enableBelowNameHealth(Player player) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = mainBoard.getObjective(belowNameObjectiveName);
        if (objective != null) {
            objective.setDisplaySlot(DisplaySlot.BELOW_NAME); // Set for everyone
            playersWithBelowNameHealth.add(player.getUniqueId()); // Track intent (though it's global)
            plugin.getLogger().fine("Enabled below-name health display globally.");
        } else {
            plugin.getLogger().warning("Attempted to enable below-name health, but objective is missing.");
            setupBelowNameObjective(); // Try to set it up again
            objective = mainBoard.getObjective(belowNameObjectiveName);
            if(objective != null) objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }
    }

    // More accurately, this hides it globally since it modifies the main scoreboard slot
    public void disableBelowNameHealth(Player player) {
        Scoreboard mainBoard = Bukkit.getScoreboardManager().getMainScoreboard();
        Objective objective = mainBoard.getObjective(DisplaySlot.BELOW_NAME); // Get objective currently in slot
        if (objective != null && objective.getName().equals(belowNameObjectiveName)) {
            mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            plugin.getLogger().fine("Disabled below-name health display globally.");
        }
        playersWithBelowNameHealth.remove(player.getUniqueId()); // Track intent
    }

    // Apply the current global state to a player on join/reload
    public void applyBelowNameState(Player player) {
        // The actual display is global via main scoreboard slot.
        // We just track the intended state per player if needed elsewhere.
        if (configManager.isBelowNameHealthEnabled()) {
            // Ensure the objective is displayed globally
            enableBelowNameHealth(player); // This sets the global slot if needed
        } else {
            // Ensure the objective is NOT displayed globally
            disableBelowNameHealth(player); // This clears the global slot if needed
        }
    }


    // --- Tab List Health ---

    private void startTabUpdateTask() {
        stopTabUpdateTask(); // Ensure only one task runs
        int intervalSeconds = configManager.getTabHealthUpdateIntervalSeconds();

        if (configManager.isTabHealthEnabled() && intervalSeconds > 0) {
            long intervalTicks = intervalSeconds * 20L;
            tabUpdateTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Update only players who have it enabled via the command/join logic
                    for (UUID uuid : new HashSet<>(playersWithTabHealth)) { // Iterate copy to avoid ConcurrentModificationException
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null && p.isOnline()) {
                            updateTabHealth(p);
                        } else {
                            // Player logged off, remove them
                            playersWithTabHealth.remove(uuid);
                        }
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 20L, intervalTicks); // Run async for player list name
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
        updateTabHealth(player); // Update immediately
        // Ensure task is running if globally enabled
        if (configManager.isTabHealthEnabled() && (tabUpdateTask == null || tabUpdateTask.isCancelled())) {
            startTabUpdateTask();
        }
    }

    public void disableTabHealth(Player player) {
        playersWithTabHealth.remove(player.getUniqueId());
        // Reset player list name to default (or potentially integrate with other plugins like prefixes)
        player.setPlayerListName(null); // Reset to default Bukkit behavior
    }

    // Update a specific player's tab list name
    public void updateTabHealth(Player player) {
        if (!player.isOnline() || !playersWithTabHealth.contains(player.getUniqueId())) {
            // Don't update if offline or if they have it disabled
            // Ensure name is reset if they had it enabled but shouldn't anymore
            if (!playersWithTabHealth.contains(player.getUniqueId()) && player.getPlayerListName() != null && player.getPlayerListName().contains("❤")) { // Basic check if we set it
                player.setPlayerListName(null);
            }
            return;
        }
        if (!configManager.isTabHealthEnabled()) {
            // Globally disabled, ensure it's reset
            player.setPlayerListName(null);
            return;
        }


        double currentHealth = player.getHealth();
        // Use Attribute.GENERIC_MAX_HEALTH for compatibility across versions >= 1.9
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        String healthColor = PlayerUtil.getHealthColor(currentHealth, maxHealth, configManager);
        String format = configManager.getTabHealthFormat();

        String formattedName = format
                .replace("{name}", player.getName()) // Use original name
                .replace("{health}", String.format("%.1f", currentHealth)) // Format health
                .replace("{max_health}", String.format("%.1f", maxHealth))
                .replace("{health_color}", healthColor);

        formattedName = ChatColor.translateAlternateColorCodes('&', formattedName);

        // Set player list name (runs async is fine)
        try {
            player.setPlayerListName(formattedName);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error setting player list name for " + player.getName(), e);
        }
    }

    // Apply current global state on join/reload
    public void applyTabHealthState(Player player) {
        if (configManager.isTabHealthEnabled()) {
            enableTabHealth(player); // Add to tracked set and update
        } else {
            disableTabHealth(player); // Remove from tracked set and reset name
        }
    }

    // Called on reload to apply changes and restart task
    public void reload() {
        stopTabUpdateTask(); // Stop existing task
        if (configManager.isTabHealthEnabled()) {
            startTabUpdateTask(); // Start new task if enabled
            // Re-apply state to all online players based on current config
            playersWithTabHealth.clear(); // Clear old state tracking
            for (Player p : Bukkit.getOnlinePlayers()) {
                enableTabHealth(p); // Enable for everyone according to new config state
            }
        } else {
            // Reset for all online players if disabled
            for (UUID uuid : new HashSet<>(playersWithTabHealth)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) disableTabHealth(p);
            }
            playersWithTabHealth.clear();
        }

        // Reload below-name state too
        setupBelowNameObjective(); // Re-ensure objective exists and slot is correct
        for(Player p : Bukkit.getOnlinePlayers()) {
            applyBelowNameState(p);
        }
    }

    // Called on plugin disable
    public void cleanup() {
        stopTabUpdateTask();
        // Reset names for tracked players
        for (UUID uuid : playersWithTabHealth) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.setPlayerListName(null);
        }
        playersWithTabHealth.clear();
        playersWithBelowNameHealth.clear(); // Clear this too

        // Clear below name objective globally if we own it
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            Scoreboard mainBoard = manager.getMainScoreboard();
            Objective objective = mainBoard.getObjective(DisplaySlot.BELOW_NAME);
            if (objective != null && objective.getName().equals(belowNameObjectiveName)) {
                mainBoard.clearSlot(DisplaySlot.BELOW_NAME);
            }
            // Optionally unregister objective if no other plugin might use it
            // Objective objToUnregister = mainBoard.getObjective(belowNameObjectiveName);
            // if (objToUnregister != null) try { objToUnregister.unregister(); } catch (IllegalStateException e) {}
        }
    }

}