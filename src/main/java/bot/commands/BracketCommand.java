package bot.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.util.*;

public class BracketCommand extends ListenerAdapter {
    private static final String ROUNDS_FILE = "data/tournament/rounds.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("bracket")) return;

        ObjectMapper mapper = new ObjectMapper();

        try {
            File file = new File(ROUNDS_FILE);
            if (!file.exists()) {
                event.reply("❌ No tournament data found.").setEphemeral(true).queue();
                return;
            }

            Map<String, Object> data = mapper.readValue(file, new TypeReference<>() {});
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) data.get("rounds");

            // Flat list of columns
            List<List<String>> bracketLines = new ArrayList<>();
            int maxMatchLines = 4;

            for (Map<String, Object> round : rounds) {
                List<Map<String, Object>> matches = (List<Map<String, Object>>) round.get("matches");
                List<String> column = new ArrayList<>();

                for (Map<String, Object> match : matches) {
                    String team1 = formatTeam(match.get("team1"));
                    String team2 = formatTeam(match.get("team2"));
                    String winner = formatTeam(match.get("winner"));

                    column.add(team1 + " ┐");
                    column.add("     ├─ " + (winner.equals("TBD") ? "TBD" : winner));
                    column.add(team2 + " ┘");
                    column.add("");
                }

                bracketLines.add(column);
            }

            int height = bracketLines.stream().mapToInt(List::size).max().orElse(0);
            StringBuilder bracket = new StringBuilder("```md\n🏆 Tournament Bracket\n\n");

            for (int row = 0; row < height; row++) {
                for (List<String> col : bracketLines) {
                    String line = row < col.size() ? col.get(row) : "";
                    bracket.append(pad(line, 28));
                }
                bracket.append("\n");
            }

            bracket.append("```\n");
            String output = bracket.toString();
            if (output.length() > 2000) {
                event.reply("❌ Bracket too large to display in one message. Try removing excess teams.").setEphemeral(true).queue();
                return;
            }

            event.reply(output).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.reply("❌ Failed to read tournament data.").setEphemeral(true).queue();
        }
    }

    private String formatTeam(Object obj) {
        return (obj instanceof String && !((String) obj).isBlank()) ? (String) obj : "TBD";
    }

    private String pad(String str, int length) {
        if (str.length() >= length) return str;
        return str + " ".repeat(length - str.length());
    }
}
