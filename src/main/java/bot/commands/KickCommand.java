package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class KickCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("kick")) return;

        User targetUser = event.getOption("user").getAsUser();
        String targetId = targetUser.getId();
        String requesterId = event.getUser().getId();

        event.deferReply().queue();

        File[] teams = new File("data/teams").listFiles();
        if (teams == null) {
            event.getHook().sendMessage("❌ No teams found.").queue();
            return;
        }

        for (File teamFile : teams) {
            try {
                JsonObject team = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
                JsonArray members = team.getAsJsonArray("members");

                // Find team containing the requester
                boolean isRequesterInTeam = false;
                for (JsonElement m : members) {
                    if (m.getAsJsonObject().get("discordId").getAsString().equals(requesterId)) {
                        isRequesterInTeam = true;
                        break;
                    }
                }

                if (!isRequesterInTeam) continue;

                // Check if requester is the creator (first member)
                JsonObject creator = members.get(0).getAsJsonObject();
                if (!creator.get("discordId").getAsString().equals(requesterId)) {
                    event.getHook().sendMessage("❌ Only the team creator can use this command.").queue();
                    return;
                }

                if (requesterId.equals(targetId)) {
                    event.getHook().sendMessage("❌ You cannot kick yourself.").queue();
                    return;
                }

                // Kick target if they are on the same team
                for (int i = 0; i < members.size(); i++) {
                    JsonObject member = members.get(i).getAsJsonObject();
                    if (member.get("discordId").getAsString().equals(targetId)) {
                        members.remove(i);

                        try (FileWriter writer = new FileWriter(teamFile)) {
                            new GsonBuilder().setPrettyPrinting().create().toJson(team, writer);
                        }

                        event.getHook().sendMessage("✅ Kicked <@" + targetId + "> from team `" + team.get("teamName").getAsString() + "`.").queue();
                        return;
                    }
                }

                event.getHook().sendMessage("❌ That user is not on your team.").queue();
                return;

            } catch (Exception e) {
                e.printStackTrace();
                event.getHook().sendMessage("❌ An error occurred.").queue();
                return;
            }
        }

        event.getHook().sendMessage("❌ You are not currently in a team.").queue();
    }
}
