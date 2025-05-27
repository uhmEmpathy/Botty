package bot.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.util.List;
import java.util.Map;

public class TeamsCommand extends ListenerAdapter {
    private static final String TEAMS_FOLDER = "data/teams/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("teams")) return;

        OptionMapping option = event.getOption("team");
        ObjectMapper mapper = new ObjectMapper();
        File[] files = new File(TEAMS_FOLDER).listFiles();

        if (files == null || files.length == 0) {
            event.reply("❌ No teams have been registered yet.").queue();
            return;
        }

        if (option == null) {
            // No team name provided — list all team names
            StringBuilder output = new StringBuilder("**Registered Teams:**\n\n");
            for (File file : files) {
                String name = file.getName().replace(".json", "");
                output.append("• ").append(name).append("\n");
            }
            if (output.length() > 2000) {
                event.reply("📄 Too many teams! Please narrow your search.").queue();
            } else {
                event.reply(output.toString()).queue();
            }
        } else {
            // Team name provided — show full team details
            String teamName = option.getAsString().trim().replaceAll("\\s+", " ");

            File teamFile = null;
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(teamName + ".json")) {
                    teamFile = file;
                    break;
                }
            }

            if (teamFile == null) {
                event.reply("❌ Team not found: `" + teamName + "`").setEphemeral(true).queue();
                return;
            }

            try {
                Map<String, Object> teamData = mapper.readValue(teamFile, Map.class);
                List<Map<String, Object>> players = (List<Map<String, Object>>) teamData.get("members");

                StringBuilder details = new StringBuilder();
                details.append("**").append(teamName).append("**\n\n");

                for (Map<String, Object> p : players) {
                    String discord = (String) p.get("discordName");
                    String ign = (String) p.get("leagueIGN");
                    String tier = (String) p.get("rankTier");
                    String div = (String) p.get("rankDivision");
                    int lp = (int) p.get("leaguePoints");

                    String emoji = switch (tier.toUpperCase()) {
                        case "CHALLENGER" -> "<:challenger_rank:1375125246402887772>";
                        case "GRANDMASTER" -> "<:grandmaster_rank:1375125276614197248>";
                        case "MASTER" -> "<:master_rank:1375127384751210559>";
                        case "DIAMOND" -> "<:diamond_rank:1375125292854677594>";
                        default -> "";
                    };

                    details.append("• ").append(discord).append(" (`").append(ign).append("`)\n")
                            .append("   ").append(emoji).append(" **").append(tier).append(" ").append(div).append("** (")
                            .append(lp).append(" LP)\n\n");
                }

                event.reply(details.toString()).queue();
            } catch (Exception e) {
                event.reply("❌ Failed to read team file.").setEphemeral(true).queue();
            }
        }
    }
}
