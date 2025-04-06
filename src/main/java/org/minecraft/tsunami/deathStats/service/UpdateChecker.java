package org.minecraft.tsunami.deathStats.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.scheduler.BukkitRunnable;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.config.ConfigManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

public class UpdateChecker {

    private final Main plugin;
    private final ConfigManager configManager;
    private final String currentVersion;

    public UpdateChecker(Main plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates() {
        if (!configManager.isUpdateCheckEnabled()) {
            return;
        }

        String checkUrl = configManager.getUpdateCheckUrl();
        if (checkUrl == null || checkUrl.isEmpty()) {
            plugin.getLogger().warning("Update check URL is not configured or is default in config.yml. Skipping check.");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = URI.create(checkUrl).toURL();
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        try {
                            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                            String latestVersion = json.has("latest_version") ? json.get("latest_version").getAsString() : null;
                            String downloadUrl = json.has("download_url") ? json.get("download_url").getAsString() : "N/A";
                            String spigotUrl = json.has("spigot_url") ? json.get("spigot_url").getAsString() : "N/A";

                            if (latestVersion != null && isNewerVersion(latestVersion, currentVersion)) {
                                String message = configManager.getFormattedMessageNoPrefix("update-available", "",
                                        "latest_version", latestVersion,
                                        "current_version", currentVersion,
                                        "download_url", spigotUrl
                                );
                                plugin.getLogger().warning(message);
                            } else if (latestVersion != null) {
                                plugin.getLogger().info("DeathStats is up to date (Version: " + currentVersion + ")");
                            } else {
                                plugin.getLogger().warning("Could not find 'latest_version' in the update check JSON response.");
                            }

                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to parse update check JSON response.", e);
                        }

                    } else {
                        plugin.getLogger().warning("Update check failed: HTTP error code " + responseCode + " from " + checkUrl);
                    }
                    connection.disconnect();
                } catch (IOException e) {
                    plugin.getLogger().warning(configManager.getMessage("update-check-failed", "&cCould not check for DeathStats updates."));
                    plugin.getLogger().log(Level.FINE, "Update check IO Exception: ", e);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "An unexpected error occurred during update check.", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        try {
            String[] latestParts = latestVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            int length = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int latestPart = (i < latestParts.length) ? Integer.parseInt(latestParts[i]) : 0;
                int currentPart = (i < currentParts.length) ? Integer.parseInt(currentParts[i]) : 0;
                if (latestPart > currentPart) return true;
                if (latestPart < currentPart) return false;
            }
            return false;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Could not compare versions: '" + latestVersion + "' vs '" + currentVersion + "'");
            return !latestVersion.equals(currentVersion);
        }
    }
}