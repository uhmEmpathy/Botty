package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.RoleValidator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class InhouseWinnerCommand extends ListenerAdapter {

    private static final String TEAMS_FILE = "data/inhouse/teams.json";
    private static final String QUEUE_FILE = "data/inhouse/queue.json";
    private static final String PLAYER_FOLDER = "data/players/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("winner")) return;

        String userId = event.getUser().getId();
        if (!RoleValidator.isStaffOrAdmin(userId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        String winningTeam = event.getOption("team").getAsString().toLowerCase();
        if (!winningTeam.equals("blue") && !winningTeam.equals("red")) {
            event.reply("❌ Invalid team. Must be `blue` or `red`.").setEphemeral(true).queue();
            return;
        }

        File teamFile = new File(TEAMS_FILE);
        if (!teamFile.exists()) {
            event.reply("❌ No active inhouse teams found.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonObject teams = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
            JsonArray blue = teams.getAsJsonArray("blue");
            JsonArray red = teams.getAsJsonArray("red");

            JsonArray winners = winningTeam.equals("blue") ? blue : red;
            JsonArray losers = winningTeam.equals("blue") ? red : blue;

            for (JsonElement e : winners) {
                JsonObject p = e.getAsJsonObject();
                updatePlayerResult(p.get("discordId").getAsString(), true);
            }

            for (JsonElement e : losers) {
                JsonObject p = e.getAsJsonObject();
                updatePlayerResult(p.get("discordId").getAsString(), false);
            }

            // Clean up files
            new File(TEAMS_FILE).delete();
            new File(QUEUE_FILE).delete();

            event.reply("🏆 `" + winningTeam.toUpperCase() + "` team has been declared the winner! Stats updated and queue reset.").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to process winner results.").setEphemeral(true).queue();
        }
    }

    private void updatePlayerResult(String userId, boolean won) {
        File file = new File(PLAYER_FOLDER + userId + ".json");
        if (!file.exists()) return;

        try {
            JsonObject obj = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
            int wins = obj.has("inhouseWins") ? obj.get("inhouseWins").getAsInt() : 0;
            int losses = obj.has("inhouseLosses") ? obj.get("inhouseLosses").getAsInt() : 0;

            if (won) obj.addProperty("inhouseWins", wins + 1);
            else obj.addProperty("inhouseLosses", losses + 1);

            try (FileWriter writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}