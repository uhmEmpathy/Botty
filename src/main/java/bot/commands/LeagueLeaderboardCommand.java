package bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "data/players";

    private final Map<String, String> rankEmojis = Map.of(
            "CHALLENGER", "🌟",
            "GRANDMASTER", "🔥",
            "MASTER", "🥇",
            "DIAMOND", "💎",
            "EMERALD", "🍃",
            "PLATINUM", "🌿",
            "GOLD", "🥇",
            "SILVER", "🥈",
            "BRONZE", "🥉",
            "IRON", "🪨"
    );

    private EmbedBuilder buildLeaderboardEmbed(boolean refreshed) {
        File folder = new File(PLAYER_FOLDER);
        File[] files = folder.listFiles();

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

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(playerLPs.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(refreshed ? "League Leaderboard (Refreshed)" : "League Leaderboard")
                .setColor(refreshed ? Color.GREEN : Color.BLUE);

        int rank = 1;
        for (Map.Entry<String, Integer> entry : sorted.subList(0, Math.min(10, sorted.size()))) {
            String discordName = entry.getKey();
            int lp = entry.getValue();
            String tier = playerRanks.get(discordName);
            String div = playerDivisions.get(discordName);
            String ign = playerIGNs.get(discordName);
            String emoji = rankEmojis.getOrDefault(tier.toUpperCase(), "");

            embed.appendDescription(String.format(
                    "**%d.** %s (%s) - `%s %s - %d LP` %s\n",
                    rank++, discordName, ign, tier, div, lp, emoji
            ));
        }

        return embed;
    }

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

        EmbedBuilder embed = buildLeaderboardEmbed(false);
        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("refresh_leaderboard")) return;

        event.deferEdit().queue();

        EmbedBuilder embed = buildLeaderboardEmbed(true);
        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }
}
