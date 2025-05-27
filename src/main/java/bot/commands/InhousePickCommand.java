package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class InhousePickCommand extends ListenerAdapter {

    private static final String TEAMS_FILE = "data/inhouse/teams.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("pick")) return;

        String pickerId = event.getUser().getId();
        String targetName = event.getOption("discordname").getAsString();

        File teamFile = new File(TEAMS_FILE);
        if (!teamFile.exists()) {
            event.reply("❌ Teams not initialized. Use `/inhouse-start` first.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonObject teams = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
            JsonArray blue = teams.getAsJsonArray("blue");
            JsonArray red = teams.getAsJsonArray("red");
            JsonArray remaining = teams.getAsJsonArray("remaining");

            JsonObject picker = null;
            boolean isBlueCaptain = false;
            if (blue.size() > 0 && blue.get(0).getAsJsonObject().get("discordId").getAsString().equals(pickerId)) {
                picker = blue.get(0).getAsJsonObject();
                isBlueCaptain = true;
            } else if (red.size() > 0 && red.get(0).getAsJsonObject().get("discordId").getAsString().equals(pickerId)) {
                picker = red.get(0).getAsJsonObject();
            }

            if (picker == null) {
                event.reply("❌ Only captains can make picks.").setEphemeral(true).queue();
                return;
            }

            // Validate pick order based on current team sizes
            int totalPicks = blue.size() + red.size() - 2;
            int[] draftOrder = {1, 2, 2, 2, 2, 1}; // pick pattern after captains
            boolean[] bluePicks = {true, false, true, false, true, false};

            if (totalPicks >= draftOrder.length) {
                event.reply("✅ All picks have been made.").setEphemeral(true).queue();
                return;
            }

            boolean shouldPick = isBlueCaptain == bluePicks[totalPicks];
            if (!shouldPick) {
                event.reply("❌ It's not your turn to pick.").setEphemeral(true).queue();
                return;
            }

            // Find the player in remaining
            JsonObject picked = null;
            for (int i = 0; i < remaining.size(); i++) {
                JsonObject player = remaining.get(i).getAsJsonObject();
                if (player.get("discordName").getAsString().equalsIgnoreCase(targetName)) {
                    picked = player;
                    remaining.remove(i);
                    break;
                }
            }

            if (picked == null) {
                event.reply("❌ Could not find `" + targetName + "` in remaining players.").setEphemeral(true).queue();
                return;
            }

            if (isBlueCaptain) {
                blue.add(picked);
            } else {
                red.add(picked);
            }

            // Save back
            try (FileWriter writer = new FileWriter(teamFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(teams, writer);
            }

            event.reply("✅ `" + picked.get("discordName").getAsString() + "` has been picked for the " + (isBlueCaptain ? "🟦 Blue" : "🟥 Red") + " Team.").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Something went wrong with the pick.").setEphemeral(true).queue();
        }
    }
}