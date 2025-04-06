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

    // Update on respawn (health is full)
    @EventHandler(priority = EventPriority.MONITOR) // Run after other plugins might modify health
    public void onRespawn(PlayerRespawnEvent event) {
        // Delay slightly to ensure health attributes are updated
        Main.getInstance().getServer().getScheduler().runTaskLater(Main.getInstance(), () -> {
            healthDisplayManager.updateTabHealth(event.getPlayer());
        }, 1L); // 1 tick delay
    }

    // Update on health regain
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
            // Update async or sync depending on performance needs
            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                 healthDisplayManager.updateTabHealth(player);
             });
        }
    }

    // Update on damage
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() == EntityType.PLAYER) {
            Player player = (Player) event.getEntity();
             // Delay slightly for health to apply? Maybe not needed.
             Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                 healthDisplayManager.updateTabHealth(player);
             });
        }
    }
}