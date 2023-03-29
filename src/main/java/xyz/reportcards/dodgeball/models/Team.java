package xyz.reportcards.dodgeball.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class Team {
    @Getter
    String id;
    @Getter
    String displayName;
}
