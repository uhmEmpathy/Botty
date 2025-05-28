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

    // Custom Discord emotes by rank
    private final Map<String, String> rankIcons = Map.ofEntries(
            Map.entry("CHALLENGER", "<:CHALLENGER:1104083066193977425>"),
            Map.entry("GRANDMASTER", "<:GRANDMASTER:1095503610558808218>"),
            Map.entry("MASTER", "<:MASTER:1095503452618100847>"),
            Map.entry("DIAMOND", "<:DIAMOND:1095503295197479115>"),
            Map.entry("EMERALD", "<:EMERALD:1358313265729634417>"),
            Map.entry("PLATINUM", "<:PLATINUM:1095503100732784792>"),
            Map.entry("GOLD", "<:GOLD:1095502968071127051>"),
            Map.entry("SILVER", "<:SILVER:1095502856653635686>"),
            Map.entry("BRONZE", "<:BRONZE:1095502516449460224>"),
            Map.entry("IRON", "<:IRON:1095099153039773767>")
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
                String rankTier = node.get("rankTier").asText().toUpperCase();
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
            String icon = rankIcons.getOrDefault(tier, "");

            embed.appendDescription(String.format(
                    "**%d.** %s (%s) - `%s %s - %d LP` %s\n",
                    rank++, discordName, ign, tier, div, lp, icon
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
