package xyz.reportcards.dodgeball.utils.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import xyz.reportcards.dodgeball.models.Team;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class GamePlayer {

    Team team;
    UUID player;

}
