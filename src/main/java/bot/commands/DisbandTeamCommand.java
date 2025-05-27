package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;

public class DisbandTeamCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("disband_team")) return;

        event.deferReply().queue();

        String requesterId = event.getUser().getId();

        File[] teams = new File("data/teams").listFiles();
        if (teams == null) {
            event.getHook().sendMessage("❌ No teams found.").queue();
            return;
        }

        for (File teamFile : teams) {
            try {
                JsonObject team = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
                JsonArray members = team.getAsJsonArray("members");

                if (members.isEmpty()) continue;

                JsonObject creator = members.get(0).getAsJsonObject();
                if (!creator.get("discordId").getAsString().equals(requesterId)) continue;

                String teamName = team.get("teamName").getAsString();

                if (teamFile.delete()) {
                    event.getHook().sendMessage("🗑️ Your team `" + teamName + "` has been disbanded.").queue();
                } else {
                    event.getHook().sendMessage("❌ Failed to delete team file.").queue();
                }
                return;

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().sendMessage("❌ An error occurred.").queue();
                return;
            }
        }

        event.getHook().sendMessage("❌ You are not the creator of any team.").queue();
    }
}
