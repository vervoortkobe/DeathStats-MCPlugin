package org.minecraft.tsunami.deathStats.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.config.ConfigManager;
import org.minecraft.tsunami.deathStats.manager.HealthDisplayManager;
import org.minecraft.tsunami.deathStats.manager.ScoreboardHandler;

import static org.minecraft.tsunami.deathStats.command.DisableHealthCommand.handleDisableHealthCommand;
import static org.minecraft.tsunami.deathStats.command.DisableScoreboardCommand.handleDisableScoreboardCommand;
import static org.minecraft.tsunami.deathStats.command.EnableHealthCommand.handleEnableHealthCommand;
import static org.minecraft.tsunami.deathStats.command.EnableScoreboardCommand.handleEnableScoreboardCommand;
import static org.minecraft.tsunami.deathStats.command.HelpCommand.handleHelpCommand;
import static org.minecraft.tsunami.deathStats.command.ReloadCommand.handleReloadCommand;
import static org.minecraft.tsunami.deathStats.command.ResetCommand.handleResetCommand;
import static org.minecraft.tsunami.deathStats.command.SetCommand.handleSetCommand;
import static org.minecraft.tsunami.deathStats.command.TopCommand.handleTopCommand;
import static org.minecraft.tsunami.deathStats.command.CheckCommand.handleCheckCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class BaseCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final ConfigManager configManager;
    private final ScoreboardHandler scoreboardHandler;
    private final HealthDisplayManager healthDisplayManager;


    public BaseCommand(Main plugin, ConfigManager configManager, ScoreboardHandler scoreboardHandler, HealthDisplayManager healthDisplayManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.scoreboardHandler = scoreboardHandler;
        this.healthDisplayManager = healthDisplayManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            // Default to 'check self' if player, 'help' if console
            if (sender instanceof Player) {
                return handleCheckCommand(configManager, sender, args); // args is empty here
            } else {
                return handleHelpCommand(configManager, sender, label);
            }
        }

        String subCommand = args[0].toLowerCase();

        // Pass necessary dependencies to static handler methods
        return switch (subCommand) {
            case "help" -> handleHelpCommand(configManager, sender, label);
            case "check" -> handleCheckCommand(configManager, sender, args);
            case "set" -> handleSetCommand(configManager, scoreboardHandler, sender, args); // Scoreboard needed to update
            case "reset" -> handleResetCommand(configManager, scoreboardHandler, sender, args); // Scoreboard needed to update
            case "top", "lb", "leaderboard", "list" -> handleTopCommand(configManager, sender);
            case "enable" -> handleEnableCommands(sender, args); // Delegate enable logic
            case "disable" -> handleDisableCommands(sender, args); // Delegate disable logic
            case "reload" -> handleReloadCommand(configManager, scoreboardHandler, healthDisplayManager, sender);
            default -> {
                sender.sendMessage(configManager.getFormattedMessage("invalid-subcommand", "", "command", label));
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
        return switch (args[1].toLowerCase()) {
            case "scoreboard", "sb" -> handleEnableScoreboardCommand(configManager, scoreboardHandler, sender);
            case "tab", "tablist" -> handleEnableHealthCommand(configManager, healthDisplayManager, sender, "tab");
            case "belowname", "name", "healthbar" -> handleEnableHealthCommand(configManager, healthDisplayManager, sender, "belowname");
            default -> {
                sender.sendMessage(configManager.getPrefix() + "&cInvalid type. Use 'scoreboard', 'tab', or 'belowname'.");
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
        return switch (args[1].toLowerCase()) {
            case "scoreboard", "sb" -> handleDisableScoreboardCommand(configManager, scoreboardHandler, sender);
            case "tab", "tablist" -> handleDisableHealthCommand(configManager, healthDisplayManager, sender, "tab");
            case "belowname", "name", "healthbar" -> handleDisableHealthCommand(configManager, healthDisplayManager, sender, "belowname");
            default -> {
                sender.sendMessage(configManager.getPrefix() + "&cInvalid type. Use 'scoreboard', 'tab', or 'belowname'.");
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
            // Main subcommands
            Stream.of("help", "check", "top", "set", "reset", "enable", "disable", "reload")
                    .filter(cmd -> sender.hasPermission("deathstats." + cmd) || (cmd.equals("check") && sender.hasPermission("deathstats.check")) || sender.hasPermission("deathstats.admin"))
                    .forEach(possibilities::add);

        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "check":
                case "set":
                case "reset":
                    if (sender.hasPermission("deathstats." + sub) || sender.hasPermission("deathstats.admin")) {
                        Bukkit.getOnlinePlayers().forEach(p -> possibilities.add(p.getName()));
                    }
                    break;
                case "enable":
                case "disable":
                    if (sender.hasPermission("deathstats.scoreboard." + sub) || sender.hasPermission("deathstats.health."+sub) || sender.hasPermission("deathstats.admin")) {
                        possibilities.addAll(Arrays.asList("scoreboard", "tab", "belowname"));
                    }
                    break;
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            // Suggest numbers for 'set' amount
            if (sub.equals("set") && (sender.hasPermission("deathstats.set") || sender.hasPermission("deathstats.admin"))) {
                possibilities.addAll(Arrays.asList("0", "1", "10", "100"));
            }
        }

        // Filter possibilities
        String currentArg = args[args.length - 1].toLowerCase();
        for (String p : possibilities) {
            if (p.toLowerCase().startsWith(currentArg)) {
                completions.add(p);
            }
        }
        Collections.sort(completions);
        return completions;
    }
}