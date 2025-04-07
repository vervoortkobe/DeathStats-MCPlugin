package org.minecraft.tsunami.deathStats.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelpCommand {

    @SuppressWarnings("SameReturnValue")
    public static boolean handleHelpCommand(ConfigManager configManager, CommandSender sender, String label) {
        Map<String, String> commands = getStringStringMap();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getMessage("help-header", "&6--- DeathStats Help ---")));

        commands.forEach((cmdArgs, description) -> {
            String baseCommand = cmdArgs.split(" ")[0];
            String permissionNode = "deathstats." + baseCommand;
            if (cmdArgs.startsWith("check ") && cmdArgs.contains("<player>")) {
                permissionNode = "deathstats.check.others";
            } else if (cmdArgs.equals("check [player]")) {
                permissionNode = "deathstats.check";
            } else if (cmdArgs.startsWith("enable ")) {
                permissionNode = "deathstats.health.enable";
            } else if (cmdArgs.startsWith("disable ")) {
                permissionNode = "deathstats.health.disable";
            }


            if (sender.hasPermission(permissionNode) || sender.hasPermission("deathstats.admin")) {
                sender.sendMessage(configManager.getFormattedMessageNoPrefix("help-line", "&e/{command} {subcommand} &7- {description}",
                        "command", label,
                        "subcommand", cmdArgs,
                        "description", description
                ));
            }
        });

        return true;
    }

    @NotNull
    private static Map<String, String> getStringStringMap() {
        Map<String, String> commands = new LinkedHashMap<>();
        commands.put("help", "Show this help message");
        commands.put("check [player]", "Display death stats (yours or others)");
        commands.put("top", "Show leaderboard of deaths");
        commands.put("set <player> <amount>", "Set a player's deaths (Admin)");
        commands.put("reset <player>", "Reset a player's deaths (Admin)");
        commands.put("enable <type>", "Enable scoreboard, tab, or belowname health (Admin)");
        commands.put("disable <type>", "Disable scoreboard, tab, or belowname health (Admin)");
        commands.put("reload", "Reload configuration (Admin)");
        return commands;
    }
}