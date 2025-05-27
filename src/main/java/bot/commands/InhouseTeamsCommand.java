package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;

public class InhouseTeamsCommand extends ListenerAdapter {

    private static final String TEAMS_FILE = "data/inhouse/teams.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("inhouse-teams")) return;

        File teamFile = new File(TEAMS_FILE);
        if (!teamFile.exists()) {
            event.reply("❌ No teams have been formed yet. Use `/inhouse-start` to begin.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonObject teams = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
            JsonArray blue = teams.getAsJsonArray("blue");
            JsonArray red = teams.getAsJsonArray("red");

            StringBuilder response = new StringBuilder("🏆 **Current Inhouse Teams:**\n\n");

            response.append("🟦 **Blue Team:**\n");
            for (JsonElement e : blue) {
                JsonObject p = e.getAsJsonObject();
                response.append("- ").append(p.get("discordName").getAsString())
                        .append(" (").append(p.get("leagueIGN").getAsString()).append(")\n");
            }

            response.append("\n🟥 **Red Team:**\n");
            for (JsonElement e : red) {
                JsonObject p = e.getAsJsonObject();
                response.append("- ").append(p.get("discordName").getAsString())
                        .append(" (").append(p.get("leagueIGN").getAsString()).append(")\n");
            }

            event.reply(response.toString()).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to display team rosters.").setEphemeral(true).queue();
        }
    }
}