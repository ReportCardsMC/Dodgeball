package xyz.reportcards.dodgeball.utils.game;

import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.models.Arena;
import xyz.reportcards.dodgeball.models.Team;
import xyz.reportcards.dodgeball.utils.Common;
import xyz.reportcards.dodgeball.utils.Logging;

import java.util.*;

public class GameInstance {

    @Getter
    private String id;
    @Getter
    private Arena arena;
    @Getter
    private World world;
    @Getter
    private int originalPlayerCount = 0;
    @Getter
    private Map<Team, List<UUID>> teams = new HashMap<>();
    @Getter
    private List<UUID> alive = new ArrayList<>();
    @Getter
    private List<UUID> dead = new ArrayList<>();
    @Getter
    private GameStatus status;
    private BossBar gameBossBar;
    private BukkitTask task;
    private int timer = 0;

    public GameInstance(String id, Arena arena) {
        this.id = id;
        this.arena = arena;
    }

    public void setup() {
        if (!Dodgeball.getInstance().getWorldManager().cloneWorld(arena.getConfig().getWorld().getName(), "dodgeball-" + id)) {
            Logging.error("GameInstance#setup()", "Failed to clone world for game instance " + id);
            delete();
            return;
        }
        world = Bukkit.getWorld("dodgeball-" + id);
        if (world == null) {
            Logging.error("GameInstance#setup()", "Failed to get world for game instance " + id);
            delete();
            return;
        }

        status = GameStatus.WAITING;
        for (Team team : arena.getConfig().getTeams()) {
            teams.put(team, new ArrayList<>());
        }
        gameBossBar = BossBar.bossBar(MiniMessage.miniMessage().deserialize("<blue>Waiting For Players..."), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
        task = Bukkit.getScheduler().runTaskTimer(Dodgeball.getInstance(), () -> {
            checkStatus();
            checkPlayers();
        }, 0L, 20L);
    }

    private void delete() {
        if (world != null) {
            Dodgeball.getInstance().getWorldManager().deleteWorld(world.getName());
        }
        Dodgeball.getInstance().getGameHandler().removeGame(this);
    }

    public void forceEnd() {
        for (UUID uuid : getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            removePlayer(player);
        }
        delete();
    }

    private void setupGameEnd() {
        if (status != GameStatus.ENDED) {
            return;
        }

        if (timer == 0) {
            Map<Team, Integer> teamAlive = new HashMap<>();
            for (UUID alivePlayer : alive) {
                Player player = Bukkit.getPlayer(alivePlayer);
                Team team = getTeamOfPlayer(alivePlayer);
                if (player == null || team == null) {
                    continue;
                }
                teamAlive.put(team, teamAlive.getOrDefault(team, 0) + 1);
            }
            Team winningTeam = null;
            for (Map.Entry<Team, Integer> entry : teamAlive.entrySet()) {
                if (winningTeam == null || entry.getValue() > teamAlive.get(winningTeam)) {
                    winningTeam = entry.getKey();
                }
            }
        }

    }

    private void endGame() {
        if (status != GameStatus.ENDED) {
            return;
        }

        // TODO: End Game

    }

    public boolean addPlayer(Player player) { // TODO: Implement teams and team balancing
        if (status != GameStatus.WAITING || getAllPlayers().contains(player.getUniqueId()) || alive.size() >= arena.getConfig().getMaxPlayers()) {
            return false;
        }
        alive.add(player.getUniqueId());
        player.teleport(arena.getConfig().getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setMetadata("dodgeballGame", new FixedMetadataValue(Dodgeball.getInstance(), id));
        player.setGameMode(GameMode.ADVENTURE);
        player.showBossBar(gameBossBar);
        checkPlayers();
        return true;
    }

    public void killPlayer(Player player) {
        if (!alive.contains(player.getUniqueId())) {
            return;
        }
        alive.remove(player.getUniqueId());
        dead.add(player.getUniqueId());
        player.teleport(arena.getConfig().getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SPECTATOR);
        checkPlayers();
    }

    public void killPlayer(Player player, boolean addToDead) {
        if (addToDead) {
            killPlayer(player);
            return;
        }
        if (!alive.contains(player.getUniqueId())) {
            return;
        }
        removePlayer(player);
    }

    public boolean removePlayer(Player player) {
        if (!getAllPlayers().contains(player.getUniqueId())) {
            return false;
        }
        alive.remove(player.getUniqueId());
        dead.remove(player.getUniqueId());
        player.removeMetadata("dodgeballGame", Dodgeball.getInstance());
        player.teleport(Dodgeball.getInstance().getConfiguration().getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SURVIVAL);
        player.hideBossBar(gameBossBar);
        checkPlayers();
        return true;
    }

    public Team getTeamOfPlayer(UUID uuid) {
        for (Map.Entry<Team, List<UUID>> list : teams.entrySet()) {
            if (list.getValue().contains(uuid)) {
                return list.getKey();
            }
        }
        return null;
    }

    public List<UUID> getAllPlayers() {
        List<UUID> players = new ArrayList<>();
        players.addAll(alive);
        players.addAll(dead);
        return players;
    }

    private void checkPlayers() {
        for (UUID uuid : getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                alive.remove(uuid);
                dead.remove(uuid);
                Team playerTeam = getTeamOfPlayer(uuid);
                if (playerTeam != null) {
                    teams.get(playerTeam).remove(uuid);
                }
                continue;
            }

            if (player.getWorld() != world) {
                removePlayer(player);
            }

        }
    }

    private void checkStatus() {
        if (status == GameStatus.WAITING) {
            if (getAlive().size() >= 2) {
                // Start a timer
                status = GameStatus.STARTING;
                timer = 20;
                gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Starting In " + timer + " Seconds..."));
                gameBossBar.progress((float) timer / 20);
            }
        } else if (status == GameStatus.STARTING) {
            if (getAlive().size() < 2) {
                // Stop the timer
                status = GameStatus.WAITING;
                timer = 0;
                gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Waiting For Players..."));
                gameBossBar.progress(1.0f);
                return;
            }

            if (timer == 0) {
                // Start the game
                status = GameStatus.STARTED;
                timer = 300;
                gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Time Remaining: " + Common.convertSecondsToTimespan(timer)));
                gameBossBar.progress(1.0f);
                return;
            }

            timer--;
            gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Starting In " + timer + " Seconds..."));
            gameBossBar.progress((float) (20 - timer) / 20);
        } else if (status == GameStatus.STARTED) {

            if (getAlive().size() < 2 || timer == 0) {
                // Stop the timer
                status = GameStatus.ENDED;
                setupGameEnd();
                timer = 10;
                gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Ending In " + timer + " Seconds..."));
                gameBossBar.progress((float) (20 - timer) / 10);
                return;
            }

            timer--;
            gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Time Remaining: " + Common.convertSecondsToTimespan(timer)));
            gameBossBar.progress((float) timer / 300);

        } else if (status == GameStatus.ENDED) {
            if (timer == 0) {
                // End the game
                gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Game Ended"));
                gameBossBar.progress(1.0f);
                endGame();
                return;
            }

            timer--;
            gameBossBar.name(MiniMessage.miniMessage().deserialize("<blue>Ending In " + timer + " Seconds..."));
            gameBossBar.progress((float) (10 - timer) / 10);
        }
    }

}
