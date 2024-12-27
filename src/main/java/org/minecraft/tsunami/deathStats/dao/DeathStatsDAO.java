package org.minecraft.tsunami.deathStats.dao;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathStatsDAO {

    private static File deathStatsFile;
    private static YamlConfiguration config;

    public static Map<UUID, Integer> playerDeaths = new HashMap<>();
    public static boolean scoreboardEnabled = true;

    public static void initialize(File pluginFolder) {
        deathStatsFile = new File(pluginFolder, "deaths.yml");
        config = YamlConfiguration.loadConfiguration(deathStatsFile);

        if (!deathStatsFile.exists()) {
            saveDefaults();
        }
        loadDeathStats();
    }

    public static void saveDefaults() {
        config.set("deaths", null);
        config.set("scoreboardEnabled", true);
        saveConfig();
    }

    public static void saveDeathStats() {
        config.set("deaths", null);
        for (Map.Entry<UUID, Integer> entry : playerDeaths.entrySet()) {
            config.set("deaths." + entry.getKey().toString(), entry.getValue());
        }
        config.set("scoreboardEnabled", scoreboardEnabled);
        saveConfig();
    }

    public static void loadDeathStats() {
        playerDeaths.clear();
        if (config.contains("deaths")) {
            for (String key : config.getConfigurationSection("deaths").getKeys(false)) {
                playerDeaths.put(UUID.fromString(key), config.getInt("deaths." + key));
            }
        }
        scoreboardEnabled = config.getBoolean("scoreboardEnabled", true);
    }

    private static void saveConfig() {
        try {
            config.save(deathStatsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}