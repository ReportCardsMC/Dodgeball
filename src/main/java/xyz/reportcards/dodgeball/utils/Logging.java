package xyz.reportcards.dodgeball.utils;

import org.bukkit.Bukkit;

public class Logging {

    public static void log(String log) {
        Bukkit.getLogger().info("[Dodgeball] " + log);
    }

    public static void error(String where, String log) {
        Bukkit.getLogger().severe("[Dodgeball] %s: %s".formatted(where, log));
    }

}
