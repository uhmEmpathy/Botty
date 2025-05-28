package bot.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "Botty/data/players";

    private List<Map<String, Object>> getAllPlayers() {
        List<Map<String, Object>> players = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        File folder = new File(PLAYER_FOLDER);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));

        if (files == null) {
            System.out.println("No player files found in: " + folder.getAbsolutePath());
            return players;
        }

        for (File file : files) {
            try {
                Map<String, Object> data = mapper.readValue(file, new TypeReference<>() {});
                if (data.get("leaguePoints") != null &&
                        data.get("rankTier") != null &&
                        data.get("discordName") != null &&
                        data.get("leagueIGN") != null) {

                    players.add(data);
                } else {
                    System.out.println("Skipping file with missing fields: " + file.getName());
                }
            } catch (Exception e) {
                System.out.println("Failed to parse: " + file.getName());
                e.printStackTrace();
            }
        }
        return players;
    }

    private String buildLeaderboard(int limit) {
        List<Map<String, Object>> players = getAllPlayers();

        players.sort((a, b) -> {
            int lpA = Integer.parseInt(a.get("leaguePoints").toString());
            int lpB = Integer.parseInt(b.get("leaguePoints").toString());
            return Integer.compare(lpB, lpA);
        });

        return players.stream()
                .limit(limit)
                .map(p -> {
                    String name = p.get("discordName").toString();
                    String ign = p.get("leagueIGN").toString();
                    String rank = p.get("rankTier").toString();
                    String div = p.getOrDefault("rankDivision", "").toString();
                    String lp = p.get("leaguePoints").toString();
                    return String.format("**%s** (%s) - `%s %s - %s LP`", name, ign, rank, div, lp);
                })
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("league-leaderboard")) return;

        event.deferReply().queue();

        String leaderboard = buildLeaderboard(10);
        if (leaderboard.isEmpty()) leaderboard = "No players found.";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard")
                .setDescription(leaderboard)
                .setColor(Color.BLUE);

        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("refresh_leaderboard")) return;

        event.deferEdit().queue();

        String leaderboard = buildLeaderboard(10);
        if (leaderboard.isEmpty()) leaderboard = "No players found.";

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard (Refreshed)")
                .setDescription(leaderboard)
                .setColor(Color.GREEN);

        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }
}
