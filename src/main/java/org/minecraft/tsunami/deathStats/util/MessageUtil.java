package org.minecraft.tsunami.deathStats.util;

import org.bukkit.ChatColor;

public class MessageUtil {

    public static String format(String message, String... replacements) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        if (replacements.length % 2 != 0) {
            return coloredMessage;
        }
        return getString(coloredMessage, replacements);
    }

    public static String getString(String coloredMessage, String[] replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            coloredMessage = coloredMessage.replace("%" + replacements[i] + "%", replacements[i + 1]);
            coloredMessage = coloredMessage.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return coloredMessage;
    }
}