package xyz.reportcards.dodgeball.models;


import lombok.Getter;

public class Arena {

    @Getter
    ArenaConfiguration config;

    public Arena(ArenaConfiguration arenaConfiguration) {
        config = arenaConfiguration;
    }

}
