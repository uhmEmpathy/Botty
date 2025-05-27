package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class InhouseStartCommand extends ListenerAdapter {

    private static final String QUEUE_FILE = "data/inhouse/queue.json";
    private static final String TEAMS_FILE = "data/inhouse/teams.json";
    private static final String PLAYER_FOLDER = "data/players/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("inhouse-start")) return;

        File queueFile = new File(QUEUE_FILE);
        if (!queueFile.exists()) {
            event.reply("❌ The inhouse queue is empty.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonArray queue = JsonParser.parseReader(new FileReader(queueFile)).getAsJsonArray();

            if (queue.size() != 10) {
                event.reply("⚠️ You need exactly 10 players in the queue to start.").setEphemeral(true).queue();
                return;
            }

            // Load player data with LP for sorting
            List<JsonObject> enrichedQueue = new ArrayList<>();
            for (JsonElement el : queue) {
                JsonObject p = el.getAsJsonObject();
                String discordId = p.get("discordId").getAsString();
                File playerFile = new File(PLAYER_FOLDER + discordId + ".json");
                if (!playerFile.exists()) continue;

                JsonObject playerJson = JsonParser.parseReader(new FileReader(playerFile)).getAsJsonObject();
                int lp = playerJson.has("leaguePoints") ? playerJson.get("leaguePoints").getAsInt() : 0;

                JsonObject enriched = new JsonObject();
                enriched.addProperty("discordId", discordId);
                enriched.addProperty("discordName", p.get("discordName").getAsString());
                enriched.addProperty("leagueIGN", p.get("leagueIGN").getAsString());
                enriched.addProperty("leaguePoints", lp);
                enrichedQueue.add(enriched);
            }

            // Sort and select captains
            enrichedQueue.sort((a, b) -> Integer.compare(b.get("leaguePoints").getAsInt(), a.get("leaguePoints").getAsInt()));
            JsonObject captain1 = enrichedQueue.remove(0);
            JsonObject captain2 = enrichedQueue.remove(0);

            JsonObject teams = new JsonObject();
            JsonArray blue = new JsonArray();
            JsonArray red = new JsonArray();

            blue.add(captain1);
            red.add(captain2);

            teams.add("blue", blue);
            teams.add("red", red);
            teams.add("remaining", new Gson().toJsonTree(enrichedQueue));

            try (FileWriter writer = new FileWriter(TEAMS_FILE)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(teams, writer);
            }

            event.reply("✅ Inhouse match started! Captains: " +
                        "🟦 Blue Team Captain: " + captain1.get("discordName").getAsString() +
                        "🟥 Red Team Captain: " + captain2.get("discordName").getAsString() + " " +
                        "Captains may now begin picking using `/pick <discordName>`").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to start the inhouse match.").setEphemeral(true).queue();
        }
    }
}