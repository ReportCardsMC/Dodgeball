package xyz.reportcards.dodgeball.models;

import lombok.Builder;
import lombok.Getter;

@Builder
public class DatabaseConfiguration {

    @Getter
    private boolean enabled;
    @Getter
    private String uri;
    @Getter
    private String database;

}
