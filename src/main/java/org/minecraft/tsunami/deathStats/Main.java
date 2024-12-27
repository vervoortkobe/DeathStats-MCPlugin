package org.minecraft.tsunami.deathStats;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.minecraft.tsunami.deathStats.command.CommandHandler;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.eventhandler.PlayerDeathHandler;
import org.minecraft.tsunami.deathStats.eventhandler.PlayerJoinHandler;
import org.minecraft.tsunami.deathStats.scoreboard.ScoreBoardHandler;

public final class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        DeathStatsDAO.initialize(getDataFolder());

        getServer().getPluginManager().registerEvents(new PlayerJoinHandler(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathHandler(), this);

        if (DeathStatsDAO.scoreboardEnabled) {
            ScoreBoardHandler.setupScoreboard();
        }
    }

    @Override
    public void onDisable() {
        DeathStatsDAO.saveDeathStats();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("deathstats") || command.getName().equalsIgnoreCase("ds")) {
            CommandHandler.DeathStatsCommandExecutor executor = new CommandHandler.DeathStatsCommandExecutor(this);
            return executor.onCommand(sender, command, label, args);
        }
        return false;
    }
}