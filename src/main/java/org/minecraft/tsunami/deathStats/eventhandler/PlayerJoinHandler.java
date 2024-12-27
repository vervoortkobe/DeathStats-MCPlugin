package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.scoreboard.ScoreBoardHandler;

import static org.bukkit.Bukkit.getServer;

public class PlayerJoinHandler implements Listener {
    private final JavaPlugin plugin;

    public PlayerJoinHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (DeathStatsDAO.scoreboardEnabled) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ScoreBoardHandler.updateScoreboard();
                event.getPlayer().setScoreboard(ScoreBoardHandler.scoreboard);
            }, 20L); // Delay of 1 second (20 ticks)
        }
    }
}
