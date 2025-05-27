package bot.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.io.FileReader;

public class ProfileCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "data/players/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("profile")) return;

        User targetUser = event.getOption("user") != null
                ? event.getOption("user").getAsUser()
                : event.getUser();

        String userId = targetUser.getId();
        File file = new File(PLAYER_FOLDER + userId + ".json");

        if (!file.exists()) {
            event.reply("❌ No profile found for <@" + userId + ">. They may need to register first.").setEphemeral(true).queue();
            return;
        }

        try {
            JsonObject player = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();

            String ign = player.has("leagueIGN") ? player.get("leagueIGN").getAsString() : "Unknown";
            String rankTier = player.has("rankTier") ? player.get("rankTier").getAsString() : "Unranked";
            String rankDiv = player.has("rankDivision") ? player.get("rankDivision").getAsString() : "";
            int lp = player.has("leaguePoints") ? player.get("leaguePoints").getAsInt() : 0;
            int wins = player.has("inhouseWins") ? player.get("inhouseWins").getAsInt() : 0;
            int losses = player.has("inhouseLosses") ? player.get("inhouseLosses").getAsInt() : 0;

            String response = String.format(
                "📜 **Profile for <@%s>** " +
                "• **League IGN:** %s " +
                "• **Rank:** %s %s (%d LP) " +
                "• 🏆 Inhouse Wins: %d " +
                "• ❌ Inhouse Losses: %d",
                userId, ign, rankTier, rankDiv, lp, wins, losses
            );

            event.reply(response).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to load profile for <@" + userId + ">").setEphemeral(true).queue();
        }
    }
}