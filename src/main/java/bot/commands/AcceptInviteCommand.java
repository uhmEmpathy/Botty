package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;

public class AcceptInviteCommand extends ListenerAdapter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("accept")) return;

        String discordId = event.getUser().getId();
        String discordTag = event.getUser().getAsTag();
        String safeTag = discordTag.replaceAll("[^a-zA-Z0-9._-]", "_");

        String teamName = event.getOption("team").getAsString().trim().toLowerCase();
        File inviteFile = new File("data/invites/" + discordId + ".json");
        File playerFile = new File("data/players/" + discordId + " - " + safeTag + ".json");

        event.deferReply().queue();

        if (!inviteFile.exists()) {
            event.getHook().sendMessage("❌ You do not have a pending invite.").queue();
            return;
        }

        if (!playerFile.exists()) {
            event.getHook().sendMessage("❌ You must register first using `/register`.").queue();
            return;
        }

        try {
            JsonObject invite = JsonParser.parseReader(new FileReader(inviteFile)).getAsJsonObject();
            String invitedTeam = invite.get("teamName").getAsString();
            String teamFileName = invite.get("teamFile").getAsString();

            if (!invitedTeam.equalsIgnoreCase(teamName)) {
                event.getHook().sendMessage("❌ Team name does not match your invite. You were invited to `" + invitedTeam + "`.").queue();
                return;
            }

            File teamFile = new File("data/teams/" + teamFileName);
            if (!teamFile.exists()) {
                event.getHook().sendMessage("❌ The team you were invited to no longer exists.").queue();
                inviteFile.delete();
                return;
            }

            JsonObject team = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
            JsonArray members = team.getAsJsonArray("members");

            if (members.size() >= 5) {
                event.getHook().sendMessage("❌ This team is already full.").queue();
                inviteFile.delete();
                return;
            }

            JsonObject player = JsonParser.parseReader(new FileReader(playerFile)).getAsJsonObject();
            JsonObject member = new JsonObject();
            member.addProperty("discordName", discordTag);
            member.addProperty("discordId", discordId);
            member.addProperty("leagueIGN", player.get("leagueIGN").getAsString());
            member.addProperty("leagueUID", player.get("leagueUID").getAsString());

            members.add(member);

            FileWriter writer = new FileWriter(teamFile);
            gson.toJson(team, writer);
            writer.close();

            inviteFile.delete();

            event.getHook().sendMessage("✅ You have successfully joined team **" + teamName + "**.").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Failed to accept invite.").queue();
        }
    }
}
