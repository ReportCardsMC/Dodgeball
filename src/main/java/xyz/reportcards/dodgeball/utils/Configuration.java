package xyz.reportcards.dodgeball.utils;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import xyz.reportcards.dodgeball.models.Arena;
import xyz.reportcards.dodgeball.models.ArenaConfiguration;
import xyz.reportcards.dodgeball.models.Team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    @Getter
    private final Plugin plugin;
    @Getter
    private final FileConfiguration config;
    @Getter
    private final Location lobbyLocation;
    @Getter
    private final String prefix;
    @Getter
    Map<String, Arena> arenas;

    public Configuration(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        arenas = new HashMap<>();


        prefix = config.getString("config.prefix", "<dark_red><bold>DODGEBALL<reset>");

        lobbyLocation = getLocation(config.getString("lobby.world", "world"), "lobby.location");

        for (Arena arena : getAllArenas()) {
            arenas.put(arena.getConfig().getName(), arena);
        }
    }

    private List<Arena> getAllArenas() {
        List<Arena> list = new ArrayList<>();
        if (!config.isConfigurationSection("arenas")) {
            Logging.error("Configuration#getArena", "There are no arenas setup in the config");
            return list;
        }

        for (String key : config.getConfigurationSection("arenas").getKeys(false)) {
            String configPath = "arenas." + key;
            Arena arena = getArena(key, configPath);
            if (arena != null) {
                list.add(arena);
            }
        }

        if (list.isEmpty()) {
            Logging.error("Configuration#getArena", "There are no arenas setup in the config");
        }

        return list;
    }

    private Arena getArena(String name, String configPath) {
        if (!config.getBoolean("%s.enabled".formatted(configPath), true)) {
            return null;
        }

        String worldName = config.getString("%s.world".formatted(configPath));
        if (worldName == null) {
            Logging.error("Configuration#getArena", "World doesn't exist for arena in config path \"%s\"".formatted(configPath + ".world"));
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Logging.error("Configuration#getArena", "World doesn't exist for arena in config path \"%s\"".formatted(configPath + ".world"));
            return null;
        }

        Location lobby = getLocation(world.getName(), "%s.locations.waiting".formatted(configPath));
        if (lobby == null) {
            Logging.error("Configuration#getArena", "Location isn't set properly in config path \"%s\"".formatted(configPath + ".locations.waiting"));
            return null;
        }

        if (!config.isConfigurationSection("%s.teams".formatted(configPath))) {
            Logging.error("Configuration#getArena", "Teams aren't setup properly in config path \"%s\"".formatted(configPath + ".teams"));
            return null;
        }

        List<Team> teams = new ArrayList<>();
        Map<Team, Location> teamSpawns = new HashMap<>();

        for (String key : config.getConfigurationSection("%s.teams".formatted(configPath)).getKeys(false)) {
            if (!config.isString("%s.display".formatted(configPath + ".teams." + key))) {
                Logging.error("Configuration#getArena", "Team \"%s\" isn't setup properly in path \"%s\"".formatted(key, configPath + ".teams." + key));
                return null;
            }
            Team team = new Team(key, config.getString("%s.display".formatted(configPath + ".teams." + key)));
            teams.add(team);

            String teamLocationPath = "%s.locations.spawns.%s".formatted(configPath, key);
            Location location = getLocation(world.getName(), teamLocationPath);

            if (location == null) {
                Logging.error("Configuration#getArena", "Location for team \"%s\" spawn is not set properly".formatted(key));
                return null;
            }

            teamSpawns.put(team, location);
        }

        ArenaConfiguration configuration = ArenaConfiguration.builder()
                .maxPlayers(config.getInt("%s.max-players".formatted(configPath), 20))
                .ballsPerPlayer(config.getInt("%s.balls-per-player".formatted(configPath), 1))
                .deathY(config.getInt("%s.death-y".formatted(configPath), 0))
                .victoryCommand(config.getString("%s.command".formatted(configPath), ""))
                .world(world)
                .lobbyLocation(lobby)
                .teams(teams)
                .teamSpawns(teamSpawns)
                .name(name)
                .build();
        return new Arena(configuration);
    }

    private Location getLocation(String world, String configPath) {
        World worldObject = Bukkit.getWorld(world);
        if (worldObject == null) {
            Logging.error("Configuration#getLocation", "World doesn't exist in configuration path \"%s\"".formatted(configPath));
            return null;
        }

        if (
                !config.isDouble("%s.x".formatted(configPath))
                || !config.isDouble("%s.y".formatted(configPath))
                || !config.isDouble("%s.z".formatted(configPath))
        ) {
            Logging.error("Configuration#getLocation", "Location isn't set properly for path \"%s\", make sure the values \"x\", \"y\", and \"z\" are set to numbers.".formatted(configPath));
            return null;
        }

        double x = config.getDouble("%s.x".formatted(configPath));
        double y = config.getDouble("%s.y".formatted(configPath));
        double z = config.getDouble("%s.z".formatted(configPath));

        if (config.isDouble("%s.pitch".formatted(configPath)) && config.isDouble("%s.yaw".formatted(configPath))) {
            float pitch = (float) config.getDouble("%s.pitch".formatted(configPath));
            float yaw = (float) config.getDouble("%s.yaw".formatted(configPath));
            return new Location(worldObject, x, y, z, yaw, pitch);
        }

        return new Location(worldObject, x, y, z);
    }

}
