package org.minecraft.tsunami.deathStats.util;

import org.bukkit.ChatColor;

public class MainUtil {
    public static ChatColor getColorForRank(int rank) {
        return switch (rank) {
            case 1 -> ChatColor.YELLOW;
            case 2 -> ChatColor.GRAY;
            case 3 -> ChatColor.GOLD;
            default -> ChatColor.WHITE;
        };
    }
}
