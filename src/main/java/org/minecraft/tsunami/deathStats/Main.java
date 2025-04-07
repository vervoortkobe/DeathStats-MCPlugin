package org.minecraft.tsunami.deathStats;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.minecraft.tsunami.deathStats.command.BaseCommand;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.eventhandler.PlayerDeathHandler;
import org.minecraft.tsunami.deathStats.eventhandler.PlayerJoinQuitHandler;
import org.minecraft.tsunami.deathStats.eventhandler.HealthListener;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;
import org.minecraft.tsunami.deathStats.service.UpdateChecker;

import java.util.Objects;

public final class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private ScoreboardHandler scoreboardHandler;
    private HealthDisplayManager healthDisplayManager;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadConfig();

        DeathStatsDAO.initialize(getDataFolder(), getLogger());

        scoreboardHandler = new ScoreboardHandler(this, configManager);
        healthDisplayManager = new HealthDisplayManager(this, configManager);

        scoreboardHandler.setupScoreboard();

        getServer().getPluginManager().registerEvents(new PlayerJoinQuitHandler(scoreboardHandler, healthDisplayManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathHandler(configManager, scoreboardHandler), this);
        getServer().getPluginManager().registerEvents(new HealthListener(healthDisplayManager), this);


        try {
            BaseCommand baseCommand = new BaseCommand(configManager, scoreboardHandler, healthDisplayManager);
            Objects.requireNonNull(getCommand("deathstats"), "Command 'deathstats' not found in plugin.yml!")
                    .setExecutor(baseCommand);
            Objects.requireNonNull(getCommand("deathstats"), "Command 'deathstats' not found in plugin.yml!")
                    .setTabCompleter(baseCommand);
        } catch (NullPointerException e) {
            getLogger().severe("Failed to register command 'deathstats'! Make sure it's in plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new UpdateChecker(this, configManager).checkForUpdates();

        getLogger().info(configManager.getPrefix() + "DeathStats plugin v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling DeathStats...");

        DeathStatsDAO.savePlayerDeaths();

        if (scoreboardHandler != null) {
            scoreboardHandler.cleanup();
        }
        if (healthDisplayManager != null) {
            healthDisplayManager.cleanup();
        }

        getLogger().info(configManager.getPrefix() + "DeathStats plugin has been disabled!");
        instance = null;
    }

    public static Main getInstance() {
        return instance;
    }
}