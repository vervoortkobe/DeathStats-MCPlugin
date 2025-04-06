package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

import static org.minecraft.tsunami.deathStats.command.CheckCommand.handleCheckCommand;
import static org.minecraft.tsunami.deathStats.command.DisableHealthCommand.handleDisableHealthCommand;
import static org.minecraft.tsunami.deathStats.command.DisableScoreboardCommand.handleDisableScoreboardCommand;
import static org.minecraft.tsunami.deathStats.command.EnableHealthCommand.handleEnableHealthCommand;
import static org.minecraft.tsunami.deathStats.command.EnableScoreboardCommand.handleEnableScoreboardCommand;
import static org.minecraft.tsunami.deathStats.command.HelpCommand.handleHelpCommand;
import static org.minecraft.tsunami.deathStats.command.ReloadCommand.handleReloadCommand;
import static org.minecraft.tsunami.deathStats.command.ResetCommand.handleResetCommand;
import static org.minecraft.tsunami.deathStats.command.SetCommand.handleSetCommand;
import static org.minecraft.tsunami.deathStats.command.TopCommand.handleTopCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class BaseCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ConfigManager configManager;
    private final ScoreboardHandler scoreboardHandler;
    private final HealthDisplayManager healthDisplayManager;
    private final DeathStatsDAO dao; // Keep reference


    public BaseCommand(Main plugin, ConfigManager configManager, ScoreboardHandler scoreboardHandler, HealthDisplayManager healthDisplayManager, DeathStatsDAO dao) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scoreboardHandler = scoreboardHandler;
        this.healthDisplayManager = healthDisplayManager;
        this.dao = dao;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                return handleCheckCommand(configManager, dao, sender, args); // Default to check self
            } else {
                return handleHelpCommand(configManager, sender, label); // Default to help for console
            }
        }

        String subCommand = args[0].toLowerCase();

        // Pass necessary dependencies to static handler methods
        return switch (subCommand) {
            case "help" -> handleHelpCommand(configManager, sender, label);
            case "check" -> handleCheckCommand(configManager, dao, sender, args); // Pass DAO
            case "set" -> handleSetCommand(configManager, dao, scoreboardHandler, sender, args); // Pass DAO & Scoreboard Manager
            case "reset" -> handleResetCommand(configManager, dao, scoreboardHandler, sender, args); // Pass DAO & Scoreboard Manager
            case "top", "lb", "leaderboard", "list" -> handleTopCommand(configManager, dao, sender); // Pass DAO
            case "enable" -> handleEnableCommands(sender, args);
            case "disable" -> handleDisableCommands(sender, args);
            case "reload" -> handleReloadCommand(plugin, configManager, dao, scoreboardHandler, healthDisplayManager, sender);
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("invalid-subcommand", "&cUnknown subcommand.", "command", label));
                yield true;
            }
        };
    }

    // Helper to handle nested enable commands
    private boolean handleEnableCommands(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "&cUsage: /ds enable <scoreboard|tab|belowname>");
            return true;
        }
        String type = args[1].toLowerCase();
        return switch (type) {
            case "scoreboard", "sb" -> handleEnableScoreboardCommand(configManager, scoreboardHandler, sender);
            case "tab", "tablist" -> handleEnableHealthCommand(configManager, healthDisplayManager, sender, "tab");
            case "belowname", "name", "healthbar" -> handleEnableHealthCommand(configManager, healthDisplayManager, sender, "belowname");
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("invalid-health-type", "&cInvalid type. Use 'scoreboard', 'tab', or 'belowname'."));
                yield true;
            }
        };
    }

    // Helper to handle nested disable commands
    private boolean handleDisableCommands(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "&cUsage: /ds disable <scoreboard|tab|belowname>");
            return true;
        }
        String type = args[1].toLowerCase();
        return switch (type) {
            case "scoreboard", "sb" -> handleDisableScoreboardCommand(configManager, scoreboardHandler, sender);
            case "tab", "tablist" -> handleDisableHealthCommand(configManager, healthDisplayManager, sender, "tab");
            case "belowname", "name", "healthbar" -> handleDisableHealthCommand(configManager, healthDisplayManager, sender, "belowname");
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("invalid-health-type", "&cInvalid type. Use 'scoreboard', 'tab', or 'belowname'."));
                yield true;
            }
        };
    }


    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> possibilities = new ArrayList<>();

        if (args.length == 1) {
            // Main subcommands - Check permissions
            Stream.of("help", "check", "top", "set", "reset", "enable", "disable", "reload")
                    .filter(cmd -> hasPermissionForCommand(sender, cmd))
                    .forEach(possibilities::add);

        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "check":
                    // Suggest online players if checking others is allowed
                    if (hasPermissionForCommand(sender, "check.others")) {
                        Bukkit.getOnlinePlayers().forEach(p -> possibilities.add(p.getName()));
                    }
                    break;
                case "set":
                case "reset":
                    // Suggest online players if command allowed
                    if (hasPermissionForCommand(sender, sub)) {
                        Bukkit.getOnlinePlayers().forEach(p -> possibilities.add(p.getName()));
                    }
                    break;
                case "enable":
                case "disable":
                    // Suggest types if any related permission is held
                    if (hasPermissionForCommand(sender, "scoreboard." + sub) || hasPermissionForCommand(sender, "health." + sub)) {
                        possibilities.addAll(Arrays.asList("scoreboard", "tab", "belowname"));
                    }
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            // Suggest numbers for 'set' amount
            if (sub.equals("set") && hasPermissionForCommand(sender, "set")) {
                possibilities.addAll(Arrays.asList("0", "1", "10")); // Simple suggestions
            }
        }

        // Filter possibilities based on current input
        String currentArg = args[args.length - 1].toLowerCase();
        for (String p : possibilities) {
            if (p.toLowerCase().startsWith(currentArg)) {
                completions.add(p);
            }
        }
        Collections.sort(completions);
        return completions;
    }

    // Helper to check permissions cleanly, including admin override
    private boolean hasPermissionForCommand(CommandSender sender, String commandNode) {
        // Special case for self-check
        if (commandNode.equals("check") && sender instanceof Player) {
            return sender.hasPermission("deathstats.check") || sender.hasPermission("deathstats.admin");
        }
        return sender.hasPermission("deathstats." + commandNode) || sender.hasPermission("deathstats.admin");
    }
}