package bot.commands;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.util.*;

public class AdvanceTeamCommand extends ListenerAdapter {
    private static final String ROUNDS_FILE = "data/tournament/rounds.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("advance_team")) return;

        OptionMapping option = event.getOption("team");
        if (option == null) {
            event.reply("❌ You must specify a team name.").setEphemeral(true).queue();
            return;
        }

        String teamToAdvance = option.getAsString().trim().toLowerCase();
        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String, Object> data = mapper.readValue(new File(ROUNDS_FILE), new TypeReference<>() {});
            List<Map<String, Object>> rounds = (List<Map<String, Object>>) data.get("rounds");

            boolean advanced = false;

            for (int i = rounds.size() - 1; i >= 0; i--) {
                Map<String, Object> round = rounds.get(i);
                List<Map<String, Object>> matches = (List<Map<String, Object>>) round.get("matches");

                for (Map<String, Object> match : matches) {
                    String team1 = (String) match.get("team1");
                    String team2 = (String) match.get("team2");
                    Object winner = match.get("winner");

                    if ((team1 != null && team1.toLowerCase().equals(teamToAdvance)) ||
                            (team2 != null && team2.toLowerCase().equals(teamToAdvance))) {

                        if (winner != null) {
                            event.reply("⚠️ That team has already been marked as the winner.").setEphemeral(true).queue();
                            return;
                        }

                        match.put("winner", teamToAdvance);
                        advanced = true;

                        if (i + 1 < rounds.size()) {
                            List<Map<String, Object>> nextMatches = (List<Map<String, Object>>) rounds.get(i + 1).get("matches");
                            for (Map<String, Object> nextMatch : nextMatches) {
                                if (nextMatch.get("team1") == null) {
                                    nextMatch.put("team1", team1.toLowerCase().equals(teamToAdvance) ? team1 : team2);
                                    break;
                                } else if (nextMatch.get("team2") == null) {
                                    nextMatch.put("team2", team1.toLowerCase().equals(teamToAdvance) ? team1 : team2);
                                    break;
                                }
                            }
                        }

                        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(ROUNDS_FILE), data);

                        if (i + 1 == rounds.size()) {
                            event.reply("🏆 " + (team1.toLowerCase().equals(teamToAdvance) ? team1 : team2) + " has won the tournament!").queue();
                        } else {
                            event.reply("✅ " + (team1.toLowerCase().equals(teamToAdvance) ? team1 : team2) + " has advanced to the next round.").queue();
                        }

                        return;
                    }
                }
            }

            if (!advanced) {
                event.reply("❌ Team not found in any current matchups.").setEphemeral(true).queue();
            }

        } catch (Exception e) {
            event.reply("❌ Failed to process rounds file.").setEphemeral(true).queue();
        }
    }
}
