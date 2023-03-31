package xyz.reportcards.dodgeball.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class Particles {

    public static void deathParticles(Player target) {
        Location loc = target.getLocation().clone().add(0,1,0);
        World world = loc.getWorld();
        world.spawnParticle(Particle.BLOCK_CRACK, loc, 50, 0.15, 0.6, 0.15, Material.REDSTONE_BLOCK.createBlockData());
        world.spawnParticle(Particle.CRIT, loc, 10, 0.15, 0.6, 0.15);
    }

}
