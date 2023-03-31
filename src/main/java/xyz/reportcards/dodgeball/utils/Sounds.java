package xyz.reportcards.dodgeball.utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Sounds {

    public static void ding(Player target, Float volume, Float pitch) {
        target.playSound(target, Sound.BLOCK_NOTE_BLOCK_PLING, volume, pitch);
    }

    public static void playDeathCrackSound(Player target) {
        target.getWorld().playSound(target, Sound.ENTITY_TURTLE_EGG_BREAK, 0.25f, 1f);
        target.getWorld().playSound(target, Sound.BLOCK_FUNGUS_BREAK, 0.2f, 1f);
        target.getWorld().playSound(target, Sound.ENTITY_PLAYER_SPLASH, 0.1f, 0.75f);
    }

}
