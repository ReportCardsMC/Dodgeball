package xyz.reportcards.dodgeball.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

@AllArgsConstructor()
@Builder()
public class ArenaConfiguration {
    @Getter
    String name;
    @Getter
    int maxPlayers;
    @Getter
    int ballsPerPlayer;
    @Getter
    int deathY;
    @Getter
    World world;
    @Getter
    List<Team> teams;
    @Getter
    Location lobbyLocation;
    @Getter
    Map<Team, Location> teamSpawns;

    @Getter
    String victoryCommand;
}
