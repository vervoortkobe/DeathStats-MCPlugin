package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

public class PlayerJoinQuitHandler implements Listener {

    private final ScoreboardHandler scoreboardHandler;
    private final HealthDisplayManager healthDisplayManager;

    public PlayerJoinQuitHandler(ScoreboardHandler scoreboardHandler, HealthDisplayManager healthDisplayManager) {
        this.scoreboardHandler = scoreboardHandler;
        this.healthDisplayManager = healthDisplayManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Use a small delay to ensure player is fully loaded and potentially avoid conflicts
        // You might not need the delay for scoreboards/health if done correctly
        // Main.getInstance().getServer().getScheduler().runTaskLater(Main.getInstance(), () -> {
        scoreboardHandler.applyScoreboard(player); // Apply death scoreboard if enabled
        healthDisplayManager.applyBelowNameState(player); // Apply below-name health if enabled
        healthDisplayManager.applyTabHealthState(player); // Apply tab health if enabled
        // }, 5L); // 5 tick delay
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Clean up player-specific things if necessary
        healthDisplayManager.disableTabHealth(player); // Stop tracking for tab updates
        // No need to explicitly remove scoreboard or below-name, server handles that
    }
}