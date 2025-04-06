package org.minecraft.tsunami.deathStats.command;

import org.bukkit.command.CommandSender;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

public class EnableScoreboardCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleEnableScoreboardCommand(ConfigManager configManager, ScoreboardHandler scoreboardHandler, CommandSender sender) {
        if (!sender.hasPermission("deathstats.scoreboard.enable") && !sender.hasPermission("deathstats.admin")) {
            sender.sendMessage(configManager.getFormattedMessage("no-permission", "&cNo permission."));
            return true;
        }

        boolean success = configManager.saveBooleanSetting("scoreboard.enabled", true);

        if (success) {
            scoreboardHandler.reload();
            sender.sendMessage(configManager.getFormattedMessage("scoreboard-enabled", "&aScoreboard enabled."));
        } else {
            sender.sendMessage(configManager.getFormattedMessage("config-set-error", "&cFailed to save setting.", "key", "scoreboard.enabled"));
        }
        return true;
    }
}