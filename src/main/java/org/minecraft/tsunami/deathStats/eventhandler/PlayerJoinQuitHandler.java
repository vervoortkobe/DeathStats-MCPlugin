package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.minecraft.tsunami.deathStats.Main;
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
        Main.getInstance().getServer().getScheduler().runTaskLater(Main.getInstance(), () -> {
            scoreboardHandler.applyScoreboard(player);
            healthDisplayManager.applyBelowNameState(player);
            healthDisplayManager.applyTabHealthState(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        healthDisplayManager.disableTabHealth(player);
    }
}