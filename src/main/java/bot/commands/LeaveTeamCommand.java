package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class LeaveTeamCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("leave_team")) return;

        event.deferReply().queue();

        String userId = event.getUser().getId();
        String userTag = event.getUser().getAsTag();

        File[] teams = new File("data/teams").listFiles();
        if (teams == null) {
            event.getHook().sendMessage("❌ No teams found.").queue();
            return;
        }

        for (File teamFile : teams) {
            try {
                JsonObject team = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
                JsonArray members = team.getAsJsonArray("members");

                for (int i = 0; i < members.size(); i++) {
                    JsonObject member = members.get(i).getAsJsonObject();
                    if (member.get("discordId").getAsString().equals(userId)) {

                        // If team only has 1 member, delete the team
                        if (members.size() == 1) {
                            if (teamFile.delete()) {
                                event.getHook().sendMessage("🗑️ You were the only member, so team `" + team.get("teamName").getAsString() + "` was deleted.").queue();
                            } else {
                                event.getHook().sendMessage("❌ Failed to delete the team.").queue();
                            }
                            return;
                        }

                        // Prevent team creator from leaving
                        JsonObject creator = members.get(0).getAsJsonObject();
                        if (creator.get("discordId").getAsString().equals(userId)) {
                            event.getHook().sendMessage("⚠️ You are the team creator. Use `/disband_team` if you want to delete the team.").queue();
                            return;
                        }

                        // Remove the user
                        members.remove(i);
                        try (FileWriter writer = new FileWriter(teamFile)) {
                            new GsonBuilder().setPrettyPrinting().create().toJson(team, writer);
                        }

                        event.getHook().sendMessage("✅ You have left team `" + team.get("teamName").getAsString() + "`.").queue();
                        return;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().sendMessage("❌ Error while processing your request.").queue();
                return;
            }
        }

        event.getHook().sendMessage("❌ You are not currently in a team.").queue();
    }
}
