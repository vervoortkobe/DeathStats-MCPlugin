package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;

public class DisableHealthCommand {

    public static boolean handleDisableHealthCommand(ConfigManager configManager, HealthDisplayManager healthManager, CommandSender sender, String type) {
        if (!sender.hasPermission("deathstats.health.disable") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        boolean success = false;
        String messageKey = "";
        String configPath = "";

        if ("tab".equals(type)) {
            configPath = "health-display.tablist.enabled";
            success = configManager.saveBooleanSetting(configPath, false);
            messageKey = "tabhealth-disabled";
        } else if ("belowname".equals(type)) {
            configPath = "health-display.below-name.enabled";
            success = configManager.saveBooleanSetting(configPath, false);
            messageKey = "belownamehealth-disabled";
        } else {
            sender.sendMessage(configManager.getFormattedMessage("invalid-health-type", "&cInvalid type."));
            return true;
        }

        if (success) {
            // Apply change via manager's reload logic
            healthManager.reload();
            sender.sendMessage(configManager.getFormattedMessage(messageKey, "&cHealth display disabled."));
        } else {
            sender.sendMessage(configManager.getFormattedMessage("config-set-error", "&cFailed to save setting.", "key", configPath));
        }
        return true;
    }
}