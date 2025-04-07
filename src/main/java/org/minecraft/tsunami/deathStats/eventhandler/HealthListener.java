package org.minecraft.tsunami.deathStats.eventhandler;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;

public class HealthListener implements Listener {

    private final HealthDisplayManager healthDisplayManager;

    public HealthListener(HealthDisplayManager healthDisplayManager) {
        this.healthDisplayManager = healthDisplayManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Main.getInstance().getServer().getScheduler().runTaskLater(Main.getInstance(), () -> {
            healthDisplayManager.updateTabHealth(event.getPlayer());
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                 healthDisplayManager.updateTabHealth(player);
             });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
             Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                 healthDisplayManager.updateTabHealth(player);
             });
        }
    }
}