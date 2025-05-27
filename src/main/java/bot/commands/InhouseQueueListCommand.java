package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;

public class InhouseQueueListCommand extends ListenerAdapter {

    private static final String QUEUE_FILE = "data/inhouse/queue.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("inhouse-queuelist")) return;

        File queueFile = new File(QUEUE_FILE);
        if (!queueFile.exists()) {
            event.reply("📭 The inhouse queue is currently empty.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonArray queue = JsonParser.parseReader(new FileReader(queueFile)).getAsJsonArray();

            if (queue.size() == 0) {
                event.reply("📭 The inhouse queue is currently empty.").setEphemeral(true).queue();
                return;
            }

            StringBuilder response = new StringBuilder("📋 **Inhouse Queue List (" + queue.size() + "/10):**\n");

            for (int i = 0; i < queue.size(); i++) {
                JsonObject player = queue.get(i).getAsJsonObject();
                response.append((i + 1)).append(". ")
                        .append(player.get("discordName").getAsString())
                        .append(" (")
                        .append(player.get("leagueIGN").getAsString())
                        .append(")\n");
            }

            event.reply(response.toString()).setEphemeral(false).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to load the queue list.").setEphemeral(true).queue();
        }
    }
}