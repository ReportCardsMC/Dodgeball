package xyz.reportcards.dodgeball.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.models.Team;
import xyz.reportcards.dodgeball.utils.Common;
import xyz.reportcards.dodgeball.utils.game.GameInstance;

public class GameListeners implements Listener {

    @EventHandler
    public void onSnowballHit(ProjectileHitEvent event) {
        if (event.getHitEntity() != null && event.getHitEntity() instanceof Player player) {
            GameInstance game = Dodgeball.getInstance().getGameHandler().findGameFromPlayer(player);
            if (game != null && game.getAlive().contains(player.getUniqueId())) {
                Team playerTeam = game.getTeamOfPlayer(player.getUniqueId());
                Team shooterTeam = game.getTeamOfPlayer(((Player) event.getEntity().getShooter()).getUniqueId());
                if (playerTeam != shooterTeam) {
                    game.killPlayer(player, (Player) event.getEntity().getShooter());
                } else {
                    Common.sendMessage((Player) event.getEntity().getShooter(), "&cYou can't hit your own team!");
                }
            }
        }
        event.getEntity().getLocation().getWorld().dropItem(
                event.getEntity().getLocation().clone().add(0,0.1,0),
                Common.dodgeballItem(),
                item -> item.setVelocity(new Vector(0,0,0))
        );
    }

}
