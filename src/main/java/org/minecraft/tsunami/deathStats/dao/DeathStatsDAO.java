package org.minecraft.tsunami.deathStats.dao;

import org.bukkit.configuration.ConfigurationSection; // Import ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger; // Use specific logger

public class DeathStatsDAO {

    private static File deathStatsFile;
    private static YamlConfiguration deathConfig;
    private static Logger logger; // Store logger instance

    // Only store player deaths here. Other settings are in config.yml via ConfigManager
    public static Map<UUID, Integer> playerDeaths = new HashMap<>();

    public static void initialize(File pluginFolder, Logger pluginLogger) {
        logger = pluginLogger; // Get logger from Main
        deathStatsFile = new File(pluginFolder, "deaths.yml");

        if (!deathStatsFile.exists()) {
            logger.info("deaths.yml not found, creating a new one...");
            try {
                // Create parent directories if they don't exist
                deathStatsFile.getParentFile().mkdirs();
                // Create the file itself
                deathStatsFile.createNewFile();
                // Load the newly created empty file
                deathConfig = YamlConfiguration.loadConfiguration(deathStatsFile);
                // Optional: Save an empty 'deaths' section to initialize structure
                deathConfig.createSection("deaths");
                saveConfig();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Could not create deaths.yml!", e);
                // Handle error appropriately, maybe disable parts of the plugin
                return;
            }
        } else {
            deathConfig = YamlConfiguration.loadConfiguration(deathStatsFile);
        }

        loadDeathStats();
    }

    // Renamed for clarity - only saves death counts
    public static void savePlayerDeaths() {
        if (deathConfig == null) {
            logger.warning("Attempted to save player deaths but deathConfig is null (maybe initialization failed?).");
            return;
        }
        // Clear existing death data before saving current map
        deathConfig.set("deaths", null); // Remove the whole section first
        ConfigurationSection deathsSection = deathConfig.createSection("deaths"); // Recreate it

        for (Map.Entry<UUID, Integer> entry : playerDeaths.entrySet()) {
            // Use the ConfigurationSection to set values
            deathsSection.set(entry.getKey().toString(), entry.getValue());
        }
        saveConfig();
        // logger.info("Saved " + playerDeaths.size() + " player death records."); // Optional: Verbose logging
    }

    // Renamed for clarity - only loads death counts
    public static void loadDeathStats() {
        playerDeaths.clear();
        if (deathConfig == null) {
            logger.warning("Attempted to load player deaths but deathConfig is null.");
            return;
        }

        ConfigurationSection deathsSection = deathConfig.getConfigurationSection("deaths");
        if (deathsSection != null) {
            for (String key : deathsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int deaths = deathsSection.getInt(key);
                    playerDeaths.put(uuid, deaths);
                } catch (IllegalArgumentException e) {
                    logger.warning("Skipping invalid UUID found in deaths.yml: " + key);
                }
            }
        }
        logger.info("Loaded " + playerDeaths.size() + " player death records.");
    }

    // Add/Update a single player's death count
    public static void incrementPlayerDeaths(UUID playerId) {
        int currentDeaths = playerDeaths.getOrDefault(playerId, 0);
        playerDeaths.put(playerId, currentDeaths + 1);
        // Consider saving immediately or batching saves
        savePlayerDeaths(); // Save after each death for data safety
    }

    public static void setPlayerDeaths(UUID playerId, int deaths) {
        if (deaths < 0) deaths = 0; // Ensure non-negative
        playerDeaths.put(playerId, deaths);
        savePlayerDeaths();
    }

    public static void resetPlayerDeaths(UUID playerId) {
        playerDeaths.put(playerId, 0);
        savePlayerDeaths();
    }

    public static int getPlayerDeaths(UUID playerId) {
        return playerDeaths.getOrDefault(playerId, 0);
    }


    private static void saveConfig() {
        if (deathConfig == null || deathStatsFile == null) {
            logger.severe("Cannot save deaths.yml - configuration or file object is null.");
            return;
        }
        try {
            deathConfig.save(deathStatsFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not save deaths.yml!", e);
        }
    }

    // --- Getters for direct access ---
    public static Map<UUID, Integer> getAllPlayerDeaths() {
        return playerDeaths; // Return reference (modify with caution) or copy if needed
    }
}