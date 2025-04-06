package org.minecraft.tsunami.deathStats.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.minecraft.tsunami.deathStats.Main;
import org.minecraft.tsunami.deathStats.dao.DeathStatsDAO;
import org.minecraft.tsunami.deathStats.scoreboard.ScoreBoardHandler;

import java.util.Map;
import java.util.UUID;
import java.util.List;

import static org.minecraft.tsunami.deathStats.util.UpdateChecker.getColorForRank;

public class CommandHandler {

    public record DeathStatsCommandExecutor(Main plugin) {

        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!command.getName().equalsIgnoreCase("deathstats") && !command.getName().equalsIgnoreCase("ds")) {
                return false;
            }

            if (args.length == 0) {
                if (sender instanceof Player player) {
                    displayPlayerStats(player, player.getUniqueId());
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "This command can only be used by players.");
                }
                return true;
            }

            return switch (args[0].toLowerCase()) {
                case "help" -> {
                    sendHelpMessage(sender);
                    yield true;
                }
                case "check" -> handleCheckCommand(sender, args);
                case "set" -> handleSetCommand(sender, args);
                case "reset" -> handleResetCommand(sender, args);
                case "top", "lb", "leaderboard", "list" -> handleTopCommand(sender);
                case "toggle" -> handleToggleCommand(sender);
                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /deathstats help for help.");
                    yield true;
                }
            };
        }

        private void sendHelpMessage(CommandSender sender) {
            sender.sendMessage(ChatColor.GOLD + "DeathStats Commands:");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats" + ChatColor.WHITE + " - Display your death stats 💀");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats help" + ChatColor.WHITE + " - Show this help message");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats top" + ChatColor.WHITE + " - Show leaderboard of deaths");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats check <player>" + ChatColor.WHITE + " - Check a player's deaths");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats set <player>" + ChatColor.WHITE + " - Set a player's deaths");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats reset <player>" + ChatColor.WHITE + " - Reset a player's deaths");
            sender.sendMessage(ChatColor.YELLOW + "/deathstats toggle" + ChatColor.WHITE + " - Toggle the scoreboard visibility");
        }

        private boolean handleCheckCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("deathstats.check")) {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to check player death stats.");
                return true;
            }

            OfflinePlayer target;
            if (args.length < 2) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Usage: /deathstats check <player>");
                    return true;
                }
                target = (OfflinePlayer) sender;
            } else {
                target = Bukkit.getOfflinePlayer(args[1]);
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(ChatColor.RED + "Player has never joined the server.");
                    return true;
                }
            }

            displayPlayerStats(sender, target.getUniqueId());
            return true;
        }

        private boolean handleSetCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("deathstats.set")) {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to set player deaths.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /deathstats set <player> <amount>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(ChatColor.RED + "Player has never joined the server.");
                return true;
            }

            int newDeaths;
            try {
                newDeaths = Integer.parseInt(args[2]);
                if (newDeaths < 0) {
                    sender.sendMessage(ChatColor.RED + "Death count cannot be negative.");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid death count. Please enter a valid number.");
                return true;
            }

            UUID targetUUID = target.getUniqueId();
            int previousDeaths = DeathStatsDAO.playerDeaths.getOrDefault(targetUUID, 0);

            DeathStatsDAO.playerDeaths.put(targetUUID, newDeaths);
            DeathStatsDAO.saveDeathStats();
            ScoreBoardHandler.updateScoreboard();

            String previousDeathWord = previousDeaths == 1 ? "death" : "deaths";
            String newDeathWord = newDeaths == 1 ? "death" : "deaths";
            sender.sendMessage(ChatColor.GREEN + "Deaths for " + target.getName() + " have been set from " +
                    ChatColor.YELLOW + previousDeaths + ChatColor.GREEN + " " + previousDeathWord + " to " +
                    ChatColor.YELLOW + newDeaths + ChatColor.GREEN + " " + newDeathWord + ".");
            return true;
        }

        private boolean handleResetCommand(CommandSender sender, String[] args) {
            if (!sender.hasPermission("deathstats.reset")) {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to reset player deaths.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /deathstats reset <player>");
                return true;
            }

            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(ChatColor.RED + "Player has never joined the server.");
                return true;
            }

            UUID targetUUID = target.getUniqueId();
            int previousDeaths = DeathStatsDAO.playerDeaths.getOrDefault(targetUUID, 0);

            DeathStatsDAO.playerDeaths.put(targetUUID, 0);
            DeathStatsDAO.saveDeathStats();
            ScoreBoardHandler.updateScoreboard();

            String deathWord = previousDeaths == 1 ? "death" : "deaths";
            sender.sendMessage(ChatColor.GREEN + "Deaths for " + target.getName() + " have been reset from " +
                    ChatColor.YELLOW + previousDeaths + ChatColor.GREEN + " " + deathWord + " to 0.");
            return true;
        }

        private boolean handleTopCommand(CommandSender sender) {
            if (DeathStatsDAO.playerDeaths.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "No death statistics available.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "☠ " + ChatColor.BOLD + "Deaths Leaderboard" + ChatColor.GOLD + " ☠");
            sender.sendMessage(ChatColor.GRAY + "------------------------");

            List<Map.Entry<UUID, Integer>> sortedEntries = DeathStatsDAO.playerDeaths.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(10)
                    .toList();

            for (int i = 0; i < 10; i++) {
                if (i < sortedEntries.size()) {
                    Map.Entry<UUID, Integer> entry = sortedEntries.get(i);
                    String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    int deaths = entry.getValue();
                    ChatColor color = getColorForRank(i + 1);

                    String displayText;
                    if (i < 3) {
                        displayText = String.format("%s%s%d. %s%s - %s",
                                color, ChatColor.BOLD, i + 1,
                                color, ChatColor.BOLD + playerName,
                                ChatColor.WHITE.toString() + deaths);
                    } else {
                        displayText = String.format("%s%d. %s%s - %s",
                                color, i + 1,
                                color, playerName,
                                ChatColor.WHITE.toString() + deaths);
                    }

                    sender.sendMessage(displayText);
                } else {
                    sender.sendMessage(ChatColor.GRAY + "...");
                    break;
                }
            }

            sender.sendMessage(ChatColor.GRAY + "------------------------");
            return true;
        }

        private boolean handleToggleCommand(CommandSender sender) {
            if (!sender.hasPermission("deathstats.toggle")) {
                sender.sendMessage(ChatColor.DARK_RED + "You don't have permission to toggle the death scoreboard.");
                return true;
            }

            DeathStatsDAO.scoreboardEnabled = !DeathStatsDAO.scoreboardEnabled;
            DeathStatsDAO.saveDeathStats();

            if (DeathStatsDAO.scoreboardEnabled) {
                ScoreBoardHandler.setupScoreboard();
                ScoreBoardHandler.updateScoreboard();
                sender.sendMessage(ChatColor.GREEN + "Death scoreboard enabled.");
            } else {
                Bukkit.getOnlinePlayers().forEach(player -> player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()));
                sender.sendMessage(ChatColor.RED + "Death scoreboard disabled.");
            }

            return true;
        }

        private void displayPlayerStats(CommandSender sender, UUID playerId) {
            int deaths = DeathStatsDAO.playerDeaths.getOrDefault(playerId, 0);
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            String playerName = player.getName() != null ? player.getName() : "Unknown Player";

            List<Map.Entry<UUID, Integer>> sortedEntries = DeathStatsDAO.playerDeaths.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .toList();

            int rank = sortedEntries.indexOf(Map.entry(playerId, deaths)) + 1;
            ChatColor rankColor = getColorForRank(rank);

            String displayDeaths = ChatColor.BOLD.toString() + deaths;

            sender.sendMessage(ChatColor.GOLD + "☠ Death Statistics for " + ChatColor.YELLOW + playerName);
            sender.sendMessage(ChatColor.WHITE + "Deaths: " + displayDeaths);
            sender.sendMessage(ChatColor.WHITE + "Rank: " + rankColor + "#" + rank);
        }

    }
}