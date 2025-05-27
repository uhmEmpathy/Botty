package data.models;

public class Player {
    public String discordId;
    public String ign;
    public String summonerId;

    public Player(String discordId, String ign, String summonerId) {
        this.discordId = discordId;
        this.ign = ign;
        this.summonerId = summonerId;
    }
}
