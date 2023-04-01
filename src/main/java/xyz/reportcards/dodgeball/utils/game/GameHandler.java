package xyz.reportcards.dodgeball.utils.game;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.models.Arena;
import xyz.reportcards.dodgeball.utils.Common;
import xyz.reportcards.dodgeball.utils.Logging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameHandler {

    @Getter
    private Map<String, GameInstance> instances = new HashMap<>();


    /**
     * Creates a new game instance
     * @param arena Arena to create the game in
     * @return The created game instance
     */
    private GameInstance createGame(Arena arena) {
        String id = Common.generateID(3);
        while (instances.containsKey(id)) {
            id = Common.generateID(3);
        }
        GameInstance gameInstance = new GameInstance(id, arena);
        gameInstance.setup();
        instances.put(id, gameInstance);
        return gameInstance;
    }

    /**
     * Remove a game from the game handler
     * @param game Game to remove
     */
    void removeGame(GameInstance game) {
        instances.remove(game.getId());
        Logging.log("Game " + game.getId() + " has been removed from the game handler.");
    }

    /**
     * Finds a game from a player
     * @param player Player to find the game from
     * @return GameInstance the player is in
     */
    public GameInstance findGameFromPlayer(Player player) {
        var metadataValue = player.getMetadata("dodgeballGame");
        if (metadataValue.isEmpty()) {
            return null;
        }
        String gameID = metadataValue.get(0).asString();
        if ("".equals(gameID)) {
            return null;
        }

        return instances.get(gameID);
    }

    /**
     * Finds a game from a player
     * @param player Player to find the game from
     * @return GameInstance the player is in
     */
    public GameInstance findGameFromPlayer(UUID player) {
        Player bukkitPlayer = Bukkit.getPlayer(player);
        if (bukkitPlayer == null) {
            return null;
        }

        return findGameFromPlayer(bukkitPlayer);
    }

    /**
     * Finds the game with the most amount of players
     * @return GameInstance with the most amount of players
     */
    public GameInstance findOpenGame() {
        GameInstance game = null;
        for (GameInstance gameInstance : instances.values()) {
            if (gameInstance.getStatus() == GameStatus.WAITING || gameInstance.getStatus() == GameStatus.STARTING) {
                if (game == null) {
                    game = gameInstance;
                } else {
                    if (game.getAlive().size() < gameInstance.getAlive().size()) {
                        game = gameInstance;
                    }
                }
            }
        }
        return game;
    }

    /**
     * Finds the game with the most amount of players in the specified arena
     * @param arena Arena to find the game in
     * @return GameInstance with the most amount of players in the specified arena
     */
    public GameInstance findOpenGame(String arena) {
        if (!Dodgeball.getInstance().getConfiguration().getArenas().containsKey(arena)) {
            Logging.error("GameHandler#findOpenGame()", "Arena " + arena + " does not exist!");
            return null;
        }

        GameInstance game = null;
        for (GameInstance gameInstance : instances.values()) {
            if ((gameInstance.getStatus() == GameStatus.WAITING  || gameInstance.getStatus() == GameStatus.STARTING) && gameInstance.getArena().getConfig().getName().equals(arena)) {
                if (game == null) {
                    game = gameInstance;
                } else {
                    if (game.getAlive().size() < gameInstance.getAlive().size()) {
                        game = gameInstance;
                    }
                }
            }
        }
        return game;
    }

    /**
     * Finds a game or creates a new one
     * @param arena Arena to find the game in
     * @return GameInstance with the most amount of players in the specified arena
     */
    public GameInstance findOrCreateGame(String arena) {
        GameInstance game = findOpenGame(arena);
        if (game == null) {
            game = createGame(Dodgeball.getInstance().getConfiguration().getArenas().get(arena));
            instances.put(game.getId(), game);
        }
        return game;
    }

    public GameInstance findOrCreateGame() {
        GameInstance game = findOpenGame();
        if (game == null) {
            // Get a random arena
            var arenaParameter = Dodgeball.getInstance().getConfiguration().getArenas().values().stream().findAny();
            if (arenaParameter.isEmpty()) {
                Logging.error("GameHandler#findOrCreateGame()", "No arenas found!");
                return null;
            }
            Arena arena = arenaParameter.get();
            game = createGame(arena);
        }
        return game;
    }


    public GameInstance getGame(String gameID) {
        return instances.get(gameID);
    }
}
