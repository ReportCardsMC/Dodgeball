package xyz.reportcards.dodgeball.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.utils.Common;
import xyz.reportcards.dodgeball.utils.game.GameInstance;
import xyz.reportcards.dodgeball.utils.game.GameStatus;

@CommandAlias("arena")
public class ArenaCommand extends BaseCommand {

    @HelpCommand
    public void help(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("join")
    @CommandCompletion("@arenas")
    public void join(Player player, @Optional String arena) {
        GameInstance gameInstance = null;
        if (arena == null) {
            gameInstance = Dodgeball.getInstance().getGameHandler().findOrCreateGame();
        } else {
            gameInstance = Dodgeball.getInstance().getGameHandler().findOrCreateGame(arena);
        }

        if (gameInstance.getAllPlayers().contains(player.getUniqueId())) {
            Common.sendMessage(player, "<red>You're already in a game!");
            return;
        }

        if (gameInstance.getStatus() != GameStatus.WAITING && gameInstance.getStatus() != GameStatus.STARTING) {
            Common.sendMessage(player, "<red>The game has already started!");
            return;
        }

        if (gameInstance.addPlayer(player)) {
            Common.sendMessage(player, "<green>You have joined the game!");
        } else {
            Common.sendMessage(player, "<red>Failed to join the game");
        }
    }

    @Subcommand("leave")
    public void leave(Player player) {
        String gameID = player.getMetadata("dodgeballGame").get(0).asString();
        if ("".equals(gameID)) {
            Common.sendMessage(player, "<red>You're not in a game!");
            return;
        }

        GameInstance gameInstance = Dodgeball.getInstance().getGameHandler().getGame(gameID);

        if (gameInstance == null) {
            Common.sendMessage(player, "<red>Failed to find the game!");
            return;
        }

        if (gameInstance.removePlayer(player)) {
            Common.sendMessage(player, "<green>You have left the game!");
        } else {
            Common.sendMessage(player, "<red>Failed to leave the game");
        }
    }

    @Subcommand("end")
    @CommandPermission("dodgeball.admin")
    @CommandCompletion("@games")
    public void end(Player player, @Optional String id) {
        if (id == null) {
            String gameID = player.getMetadata("dodgeballGame").get(0).asString();
            if ("".equals(gameID)) {
                Common.sendMessage(player, "<red>You're not in a game!");
                return;
            }
            id = gameID;
        }

        GameInstance gameInstance = Dodgeball.getInstance().getGameHandler().getGame(id);

        if (gameInstance == null) {
            Common.sendMessage(player, "<red>Failed to find the game!");
            return;
        }

        gameInstance.forceEnd();
        Common.sendMessage(player, "<green>Game ended");
    }
}
