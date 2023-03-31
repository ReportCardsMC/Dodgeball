package xyz.reportcards.dodgeball.utils.game;

import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import xyz.reportcards.dodgeball.Dodgeball;
import xyz.reportcards.dodgeball.models.Arena;
import xyz.reportcards.dodgeball.models.Team;
import xyz.reportcards.dodgeball.utils.Common;
import xyz.reportcards.dodgeball.utils.Logging;
import xyz.reportcards.dodgeball.utils.Particles;
import xyz.reportcards.dodgeball.utils.Sounds;

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
        Logging.log("Created world " + world.getName() + " for game instance " + id);
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
        task = Bukkit.getScheduler().runTaskTimer(Dodgeball.getInstance(), new Runnable() {
            int runnableTicks = 0;

            @Override
            public void run() {
                if (runnableTicks % 10 == 0) {
                    checkStatus();
                }
                checkPlayers();
                runnableTicks++;
            }
        }, 20L, 2L);
    }

    private void assignTeam(Player player) {
        Team lowestTeam = null;
        for (Team team : teams.keySet()) {
            if (lowestTeam == null) {
                lowestTeam = team;
                continue;
            }
            if (teams.get(team).size() < teams.get(lowestTeam).size()) {
                lowestTeam = team;
            }
        }
        teams.get(lowestTeam).add(player.getUniqueId());
    }

    private void gameStarted() {
        if (status.equals(GameStatus.STARTED)) {
            for (UUID uuid : getAlive()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null) {
                    continue;
                }

                player.setGameMode(GameMode.ADVENTURE);
                player.setAllowFlight(false);
                player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue());
                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setExhaustion(0);
                player.teleportAsync(teamLocation(getTeamOfPlayer(uuid)), PlayerTeleportEvent.TeleportCause.PLUGIN);
                player.getInventory().clear();
                player.getInventory().addItem(Common.dodgeballItem());
                Sounds.ding(player, 0.7f, 1.25f);
            }
        }
    }

    private void delete() {
        if (world != null) {
            Dodgeball.getInstance().getWorldManager().deleteWorld(world.getName());
        }
        if (task != null && !task.isCancelled()) {
            task.cancel();
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

        Map<Team, Integer> teamAlive = new HashMap<>();
        for (UUID alivePlayer : alive) {
            Player player = Bukkit.getPlayer(alivePlayer);
            Team team = getTeamOfPlayer(alivePlayer);
            if (player == null || team == null) {
                continue;
            }
            teamAlive.put(team, teamAlive.getOrDefault(team, 0) + 1);
            player.teleport(lobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.getInventory().clear();
        }
        Team winningTeam = null;
        for (Map.Entry<Team, Integer> entry : teamAlive.entrySet()) {
            if (winningTeam == null || entry.getValue() > teamAlive.get(winningTeam)) {
                winningTeam = entry.getKey();
            }
        }
        if (winningTeam == null) {
            tieGame();
            return;
        }

        for (UUID uuid : teams.get(winningTeam)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Common.parsePlaceholders(player, arena.getConfig().getVictoryCommand()));
        }

        winGame(winningTeam);

    }

    private void tieGame() {
        for (UUID uuid : getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            player.sendMessage(MiniMessage.miniMessage().deserialize("<blue>The game has ended in a tie!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    private void winGame(Team team) {
        for (UUID uuid : getAllPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                continue;
            }
            if (getTeamOfPlayer(uuid).equals(team)) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<blue>Your team has won the game!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<blue>Your team has lost the game!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    private void endGame() {
        if (status != GameStatus.ENDED) {
            return;
        }

        delete();
    }

    public boolean addPlayer(Player player) { // TODO: Implement teams and team balancing
        if (status != GameStatus.WAITING || getAllPlayers().contains(player.getUniqueId()) || alive.size() >= arena.getConfig().getMaxPlayers()) {
            return false;
        }
        alive.add(player.getUniqueId());
        player.teleport(lobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setMetadata("dodgeballGame", new FixedMetadataValue(Dodgeball.getInstance(), id));
        player.setGameMode(GameMode.ADVENTURE);
        player.showBossBar(gameBossBar);
        assignTeam(player);
        checkPlayers();
        Logging.log("Added player " + player.getName() + " to game instance " + id);
        return true;
    }

    public void killPlayer(Player player, Player killer) {
        if (!alive.contains(player.getUniqueId())) {
            return;
        }
        for (UUID p : getAllPlayers()) {
            Player loopPlayer = Bukkit.getPlayer(p);
            if (loopPlayer == null) {
                continue;
            }
            loopPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + player.getName() + " <gray>was killed by <red>" + killer.getName() + "<gray>."));
        }
        Particles.deathParticles(player);
        Sounds.playDeathCrackSound(player);
        alive.remove(player.getUniqueId());
        dead.add(player.getUniqueId());
        player.getInventory().clear();
        player.teleportAsync(lobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SPECTATOR);
        Logging.log("Player " + player.getName() + " died in game instance " + id);
        checkPlayers();
    }

    public void killPlayer(Player player) {
        if (!alive.contains(player.getUniqueId())) {
            return;
        }
        for (UUID p : getAllPlayers()) {
            Player loopPlayer = Bukkit.getPlayer(p);
            if (loopPlayer == null) {
                continue;
            }
            loopPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + player.getName() + " <gray>has died."));
        }
        Particles.deathParticles(player);
        Sounds.playDeathCrackSound(player);
        alive.remove(player.getUniqueId());
        dead.add(player.getUniqueId());
        player.getInventory().clear();
        player.teleportAsync(lobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.setGameMode(GameMode.SPECTATOR);
        Logging.log("Player " + player.getName() + " died in game instance " + id);
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
        Particles.deathParticles(player);
        Sounds.playDeathCrackSound(player);
        for (UUID p : getAllPlayers()) {
            Player loopPlayer = Bukkit.getPlayer(p);
            if (loopPlayer == null) {
                continue;
            }
            loopPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + player.getName() + " <gray>has died."));
        }
        removePlayer(player);
    }

    public boolean removePlayer(Player player) {
        if (!getAllPlayers().contains(player.getUniqueId())) {
            return false;
        }
        alive.remove(player.getUniqueId());
        dead.remove(player.getUniqueId());
        removeFromTeam(player);
        player.removeMetadata("dodgeballGame", Dodgeball.getInstance());
        player.teleportAsync(Dodgeball.getInstance().getConfiguration().getLobbyLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.hideBossBar(gameBossBar);
        checkPlayers();
        Logging.log("Removed player " + player.getName() + " from game instance " + id);
        return true;
    }

    private void removeFromTeam(Player player) {
        Team playerTeam = getTeamOfPlayer(player.getUniqueId());
        if (playerTeam != null) {
            teams.get(playerTeam).remove(player.getUniqueId());
        }
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

    public void checkPlayers() {
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

            if (player.getGameMode() != GameMode.ADVENTURE) {
                player.setGameMode(GameMode.ADVENTURE);
            }

            if (player.getWorld() != world) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You have been removed from the game because you left the world."));
                removePlayer(player);
                continue;
            }

            if (player.getLocation().getY() <= arena.getConfig().getDeathY()) {
                killPlayer(player);
                continue;
            }

            Block blockBelow = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockBelow.getType().isAir()) {
                // Block one more below
                blockBelow = blockBelow.getRelative(BlockFace.DOWN);
            }
            if (blockBelow.getType() == Material.OBSIDIAN) {
                // Push towards their team spawn
                Team team = getTeamOfPlayer(player.getUniqueId());
                if (team == null) {
                    continue;
                }
                Location spawn = teamLocation(team);
                Vector direction = spawn.toVector().subtract(player.getLocation().toVector()).normalize();
                player.setVelocity(direction.multiply(1.2));
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
                gameStarted();
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

    private Location teamLocation(Team team) {
        Location loc = arena.getConfig().getTeamSpawns().get(team).clone();
        loc.setWorld(world);
        return loc;
    }

    private Location lobbyLocation() {
        Location loc = arena.getConfig().getLobbyLocation().clone();
        loc.setWorld(world);
        return loc;
    }

}
