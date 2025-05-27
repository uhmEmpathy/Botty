package data.models;

import java.util.ArrayList;
import java.util.List;

public class Team {
    public String name;
    public List<String> memberDiscordIds = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }
}
