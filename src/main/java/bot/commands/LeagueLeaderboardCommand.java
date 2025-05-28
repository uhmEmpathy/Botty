package bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "data/players";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("league-leaderboard")) return;

        event.deferReply().queue();

        File folder = new File(PLAYER_FOLDER);
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            event.getHook().editOriginal("No registered player data found.").queue();
            return;
        }

        Map<String, Integer> playerLPs = new HashMap<>();
        Map<String, String> playerRanks = new HashMap<>();
        Map<String, String> playerDivisions = new HashMap<>();
        Map<String, String> playerIGNs = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();

        for (File file : files) {
            try {
                JsonNode node = mapper.readTree(file);

                String discordName = node.get("discordName").asText();
                String leagueIGN = node.get("leagueIGN").asText();
                String rankTier = node.get("rankTier").asText();
                String rankDivision = node.has("rankDivision") ? node.get("rankDivision").asText() : "";
                int lp = node.get("leaguePoints").asInt();

                playerLPs.put(discordName, lp);
                playerRanks.put(discordName, rankTier);
                playerDivisions.put(discordName, rankDivision);
                playerIGNs.put(discordName, leagueIGN);
            } catch (Exception e) {
                System.out.println("❌ Failed to parse " + file.getName());
                e.printStackTrace();
            }
        }

        if (playerLPs.isEmpty()) {
            event.getHook().editOriginal("No valid player data found.").queue();
            return;
        }

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(playerLPs.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard")
                .setColor(Color.BLUE);

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted.subList(0, Math.min(10, sorted.size()))) {
            String discordName = entry.getKey();
            int lp = entry.getValue();
            String tier = playerRanks.get(discordName);
            String div = playerDivisions.get(discordName);
            String ign = playerIGNs.get(discordName);

            embed.appendDescription(String.format("**%d.** %s (%s) - `%s %s - %d LP`\n", rank++, discordName, ign, tier, div, lp));
        }

        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }
}
