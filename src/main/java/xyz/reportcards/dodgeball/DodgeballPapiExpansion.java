package xyz.reportcards.dodgeball;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.reportcards.dodgeball.utils.game.GameInstance;

public class DodgeballPapiExpansion extends PlaceholderExpansion {


    @Override
    public @NotNull String getIdentifier() {
        return "dodgeball";
    }

    @Override
    public @NotNull String getAuthor() {
        return "ReportCards";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("team")) {
            GameInstance game = Dodgeball.getInstance().getGameHandler().findGameFromPlayer(player);
            if (game == null) {
                return "N/A";
            }
            return game.getTeamOfPlayer(player.getUniqueId()).getDisplayName();
        }
        return null;
    }
}
