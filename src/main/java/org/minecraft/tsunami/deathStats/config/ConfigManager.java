package org.minecraft.tsunami.deathStats.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.minecraft.tsunami.deathStats.Main;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import static org.minecraft.tsunami.deathStats.util.MessageUtil.getString;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;
    private File configFile;
    private String prefix; // Cache prefix

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig(); // Reloads from disk
        this.config = plugin.getConfig();
        this.prefix = getMessage("prefix", "&c&l💀 DeathStats >&r "); // Load and cache prefix
        plugin.getLogger().info("Configuration loaded.");
    }

    // Method to save specific boolean settings back to config (for enable/disable commands)
    public boolean saveBooleanSetting(String path, boolean value) {
        config.set(path, value);
        try {
            config.save(configFile);
            // No need to reload here unless other parts depend on immediate config object update
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save config.yml!", e);
            return false;
        }
    }

    // --- Getters ---

    public String getPrefix() {
        return prefix;
    }

    // Update Checker
    public boolean isUpdateCheckEnabled() {
        return config.getBoolean("update-checker.enabled", true);
    }

    public String getUpdateCheckUrl() {
        return config.getString("update-checker.url", "");
    }

    // Scoreboard
    public boolean isScoreboardEnabled() {
        return config.getBoolean("scoreboard.enabled", true);
    }

    public String getScoreboardTitle() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("scoreboard.title", "&c&l☠ Top Deaths ☠"));
    }

    public int getScoreboardEntries() {
        return config.getInt("scoreboard.entries", 5);
    }

    public String getScoreboardEntryFormat() {
        return config.getString("scoreboard.entry-format", "{rank_color}{rank}. {rank_color}{name} &f(&c{deaths}&f)");
    }

    public int getScoreboardUpdateIntervalSeconds() {
        return config.getInt("scoreboard.update-interval-seconds", 60);
    }

    // Health Display
    public boolean isTabHealthEnabled() {
        return config.getBoolean("health-display.tablist.enabled", true);
    }

    public String getTabHealthFormat() {
        return config.getString("health-display.tablist.format", "{name} {health_color}❤ {health}");
    }

    public int getTabHealthUpdateIntervalSeconds() {
        return config.getInt("health-display.tablist.update-interval-seconds", 3);
    }


    public boolean isBelowNameHealthEnabled() {
        return config.getBoolean("health-display.below-name.enabled", true);
    }

    // Health Colors
    public String getHealthColorHigh() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("health-display.health-colors.high", "&a"));
    }

    public String getHealthColorMedium() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("health-display.health-colors.medium", "&e"));
    }

    public String getHealthColorLow() {
        return ChatColor.translateAlternateColorCodes('&', config.getString("health-display.health-colors.low", "&c"));
    }

    // Messages
    public String getMessage(String key, String defaultValue) {
        String message = config.getString("messages." + key, defaultValue);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getRawMessage(String key, String defaultValue) {
        // Gets message without color codes or prefix
        return config.getString("messages." + key, defaultValue);
    }

    // Helper for formatted messages (no prefix)
    public String getFormattedMessageNoPrefix(String key, String defaultValue, String... replacements) {
        String message = getMessage(key, defaultValue);
        return replacePlaceholders(message, replacements);
    }

    // Helper for formatted messages (WITH prefix)
    public String getFormattedMessage(String key, String defaultValue, String... replacements) {
        String message = getMessage(key, defaultValue);
        return prefix + replacePlaceholders(message, replacements); // Add prefix here
    }

    private String replacePlaceholders(String message, String... replacements) {
        if (replacements.length % 2 != 0) {
            plugin.getLogger().warning("Invalid number of replacements for message. Must be key-value pairs.");
            return message;
        }
        return getString(message, replacements);
    }
}