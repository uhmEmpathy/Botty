package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class InviteCommand extends ListenerAdapter {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File inviteFolder = new File("data/invites");
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("invite")) return;

        User invitee = event.getOption("user").getAsUser();
        String inviterId = event.getUser().getId();
        String inviteeId = invitee.getId();

        event.deferReply().queue();

        try {
            // Check invitee registration
            File inviteeFile = findPlayerFile(inviteeId);
            if (inviteeFile == null) {
                event.getHook().sendMessage("❌ That user has not registered yet.").queue();
                return;
            }

            // Check if invitee already has a team
            if (findTeamContainingUser(inviteeId) != null) {
                event.getHook().sendMessage("❌ That user is already in a team.").queue();
                return;
            }

            // Check if invitee has a pending invite
            File inviteFile = new File(inviteFolder, inviteeId + ".json");
            if (inviteFile.exists()) {
                event.getHook().sendMessage("❌ That user already has a pending invite.").queue();
                return;
            }

            // Find inviter's team
            File teamFile = findTeamContainingUser(inviterId);
            if (teamFile == null) {
                event.getHook().sendMessage("❌ You must be on a team to invite others.").queue();
                return;
            }

            JsonObject teamJson = JsonParser.parseReader(new FileReader(teamFile)).getAsJsonObject();
            JsonArray members = teamJson.getAsJsonArray("members");

            if (members.size() >= 5) {
                event.getHook().sendMessage("❌ Your team already has 5 members.").queue();
                return;
            }

            // Create invites directory
            if (!inviteFolder.exists()) inviteFolder.mkdirs();

            // Save invite
            JsonObject invite = new JsonObject();
            invite.addProperty("inviterId", inviterId);
            invite.addProperty("inviterName", event.getUser().getAsTag());
            invite.addProperty("teamName", teamJson.get("teamName").getAsString());
            invite.addProperty("teamFile", teamFile.getName());

            FileWriter writer = new FileWriter(inviteFile);
            gson.toJson(invite, writer);
            writer.close();

            event.getHook().sendMessage("✅ Invite sent to `" + invitee.getAsTag() + "`. They have 60 seconds to respond.").queue();

            // Schedule auto-expire
            scheduler.schedule(() -> {
                if (inviteFile.exists()) {
                    inviteFile.delete();
                    System.out.println("Invite for " + invitee.getAsTag() + " expired.");
                }
            }, 60, TimeUnit.SECONDS);

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Failed to send invite.").queue();
        }
    }

    private File findPlayerFile(String discordId) {
        File folder = new File("data/players");
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().startsWith(discordId + " -")) return f;
        }
        return null;
    }

    private File findTeamContainingUser(String discordId) {
        File folder = new File("data/teams");
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File f : files) {
            try {
                JsonObject team = JsonParser.parseReader(new FileReader(f)).getAsJsonObject();
                JsonArray members = team.getAsJsonArray("members");
                for (JsonElement e : members) {
                    if (e.getAsJsonObject().get("discordId").getAsString().equals(discordId)) {
                        return f;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
