package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class InhouseQueueCommand extends ListenerAdapter {

    private static final String QUEUE_FILE = "data/inhouse/queue.json";
    private static final String PLAYER_FOLDER = "data/players/";
    private static final int MAX_QUEUE_SIZE = 10;
    private static final String EVENT_PLANNER_ROLE_ID = "1376227685709381723"; // Replace with actual ID

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("inhouse-queue")) return;

        String userId = event.getUser().getId();

        File playerFile = new File(PLAYER_FOLDER + userId + ".json");
        if (!playerFile.exists()) {
            event.reply("❌ You must register using `/register` before joining the inhouse queue.").setEphemeral(true).queue();
            return;
        }

        try {
            // Load player data
            JsonObject playerData = JsonParser.parseReader(new FileReader(playerFile)).getAsJsonObject();
            String riotIGN = playerData.get("leagueIGN").getAsString();

            // Load or initialize queue
            File queueFile = new File(QUEUE_FILE);
            JsonArray queue = queueFile.exists()
                ? JsonParser.parseReader(new FileReader(queueFile)).getAsJsonArray()
                : new JsonArray();

            // Check if already in queue
            for (JsonElement element : queue) {
                JsonObject entry = element.getAsJsonObject();
                if (entry.get("discordId").getAsString().equals(userId)) {
                    event.reply("⚠️ You are already in the inhouse queue.").setEphemeral(true).queue();
                    return;
                }
            }

            // Check if queue is full
            if (queue.size() >= MAX_QUEUE_SIZE) {
                event.reply("❌ The inhouse queue is full (10 players).").setEphemeral(true).queue();
                return;
            }

            // Add player to queue
            JsonObject newEntry = new JsonObject();
            newEntry.addProperty("discordId", userId);
            newEntry.addProperty("discordName", event.getUser().getAsTag());
            newEntry.addProperty("leagueIGN", riotIGN);
            queue.add(newEntry);

            try (FileWriter writer = new FileWriter(queueFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(queue, writer);
            }

            // Notify and check for full queue
            if (queue.size() == MAX_QUEUE_SIZE) {
                event.getJDA().getTextChannels().get(0).sendMessage("<@&" + EVENT_PLANNER_ROLE_ID + "> ✅ The inhouse queue is now full and ready to start!").queue();
            }

            event.reply("✅ You have been added to the inhouse queue.").setEphemeral(true).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to join the queue. Please try again.").setEphemeral(true).queue();
        }
    }
}