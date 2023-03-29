package xyz.reportcards.dodgeball.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.reportcards.dodgeball.Dodgeball;
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
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You're already in a game!"));
            return;
        }

        if (gameInstance.getStatus() != GameStatus.WAITING) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>The game has already started!"));
            return;
        }

        if (gameInstance.addPlayer(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have joined the game!"));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to join the game"));
        }
    }

    @Subcommand("leave")
    public void leave(Player player) {
        String gameID = player.getMetadata("dodgeballGame").get(0).asString();
        if ("".equals(gameID)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You're not in a game!"));
            return;
        }

        GameInstance gameInstance = Dodgeball.getInstance().getGameHandler().getGame(gameID);

        if (gameInstance == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to find the game!"));
            return;
        }

        if (gameInstance.removePlayer(player)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<green>You have left the game!"));
        } else {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to leave the game"));
        }
    }

    @Subcommand("end")
    @CommandPermission("dodgeball.admin")
    @CommandCompletion("@games")
    public void end(Player player, @Optional String id) {
        if (id == null) {
            String gameID = player.getMetadata("dodgeballGame").get(0).asString();
            if ("".equals(gameID)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You're not in a game!"));
                return;
            }
            id = gameID;
        }

        GameInstance gameInstance = Dodgeball.getInstance().getGameHandler().getGame(id);

        if (gameInstance == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Failed to find the game!"));
            return;
        }

        gameInstance.forceEnd();
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Game ended"));
    }
}
