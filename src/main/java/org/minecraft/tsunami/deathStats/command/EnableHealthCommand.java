package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;

public class EnableHealthCommand {

    public static boolean handleEnableHealthCommand(ConfigManager configManager, HealthDisplayManager healthManager, CommandSender sender, String type) {
        if (!sender.hasPermission("deathstats.health.enable") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        boolean success = false;
        String messageKey = "";
        String configPath = "";

        if ("tab".equals(type)) {
            configPath = "health-display.tablist.enabled";
            success = configManager.saveBooleanSetting(configPath, true);
            messageKey = "tabhealth-enabled";
        } else if ("belowname".equals(type)) {
            configPath = "health-display.below-name.enabled";
            success = configManager.saveBooleanSetting(configPath, true);
            messageKey = "belownamehealth-enabled";
        } else {
            // Should be caught by BaseCommand, but safety check
            sender.sendMessage(configManager.getFormattedMessage("invalid-health-type", "&cInvalid type."));
            return true;
        }

        if (success) {
            // Apply change via manager's reload logic
            healthManager.reload();
            sender.sendMessage(configManager.getFormattedMessage(messageKey, "&aHealth display enabled."));
        } else {
            sender.sendMessage(configManager.getFormattedMessage("config-set-error", "&cFailed to save setting.", "key", configPath));
        }
        return true;
    }
}