package bot.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    // Fetch and sort users by LP from JSON files
    public List<String> getSortedLeaderboard() {
        File folder = new File("Botty/data/players"); // ✅ Corrected path to your actual folder
        System.out.println("Looking for player JSONs in: " + folder.getAbsolutePath()); // Debug

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return Collections.emptyList();

        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> players = new ArrayList<>();

        for (File file : files) {
            try {
                Map<String, Object> data = mapper.readValue(file, new TypeReference<>() {});

                if (data.get("leaguePoints") == null ||
                        data.get("rankTier") == null ||
                        data.get("discordName") == null ||
                        data.get("leagueIGN") == null) {
                    System.out.println("Skipping incomplete file: " + file.getName());
                    continue;
                }

                players.add(data);
            } catch (IOException e) {
                System.out.println("Failed to read: " + file.getName());
                e.printStackTrace();
            }
        }

        // Sort by LP
        players.sort((a, b) -> {
            int lpA = Integer.parseInt(a.get("leaguePoints").toString());
            int lpB = Integer.parseInt(b.get("leaguePoints").toString());
            return Integer.compare(lpB, lpA);
        });

        List<String> leaderboard = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> p : players) {
            String line = String.format(
                    "**%d.** %s (%s) - `%s %s - %s LP`",
                    rank++,
                    p.get("discordName"),
                    p.get("leagueIGN"),
                    p.get("rankTier"),
                    p.getOrDefault("rankDivision", ""),
                    p.get("leaguePoints").toString()
            );
            leaderboard.add(line);
        }

        return leaderboard;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("league-leaderboard")) return;

        event.deferReply().queue();

        List<String> leaderboard = getSortedLeaderboard();
        String content = leaderboard.isEmpty()
                ? "No players found."
                : String.join("\n", leaderboard.subList(0, Math.min(10, leaderboard.size())));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard")
                .setDescription(content)
                .setColor(Color.BLUE);

        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("refresh_leaderboard")) return;

        event.deferEdit().queue(success -> {
            List<String> leaderboard = getSortedLeaderboard();
            String content = leaderboard.isEmpty()
                    ? "No players found."
                    : String.join("\n", leaderboard.subList(0, Math.min(10, leaderboard.size())));

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("League Leaderboard (Refreshed)")
                    .setDescription(content)
                    .setColor(Color.GREEN);

            Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

            event.getHook().editOriginalEmbeds(embed.build())
                    .setActionRow(refreshButton)
                    .queue();

        }, failure -> {
            event.reply("❌ This button interaction has expired. Please run the command again.")
                    .setEphemeral(true)
                    .queue();
        });
    }
}
