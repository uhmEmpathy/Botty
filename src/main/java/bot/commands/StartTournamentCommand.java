package bot.commands;

import api.riot.RiotApiService;
import util.RoleValidator;
import util.StaffLogger;
//import util.enums.LogType;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StartTournamentCommand extends ListenerAdapter {
    private static final String TEAMS_FOLDER = "data/teams/";
    private static final String ROUNDS_FILE = "data/tournament/rounds.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("start_tournament")) return;

        String userId = event.getUser().getId();
        if (!RoleValidator.isStaffOrAdmin(userId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        File folder = new File(TEAMS_FOLDER);
        File[] files = folder.listFiles();

        if (files == null || files.length < 2) {
            event.reply("❌ Not enough teams to start a tournament.").setEphemeral(true).queue();
            return;
        }

        List<TeamData> teams = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        List<String> skippedTeams = new ArrayList<>();

        try {
            for (File file : files) {
                Map<String, Object> teamJson = mapper.readValue(file, Map.class);

                String teamName = (String) teamJson.get("teamName");
                List<Map<String, Object>> members = (List<Map<String, Object>>) teamJson.get("members");

                if (members == null || members.size() < 5) {
                    skippedTeams.add(teamName != null ? teamName : file.getName());
                    continue;
                }

                int totalLP = members.stream()
                        .mapToInt(p -> (int) p.getOrDefault("leaguePoints", 0))
                        .sum();

                teams.add(new TeamData(teamName, totalLP));
            }
        } catch (IOException e) {
            event.reply("❌ Failed to read team files.").setEphemeral(true).queue();
            return;
        }

        if (teams.size() < 2) {
            event.reply("❌ Not enough full teams (5 members each) to start a tournament.").setEphemeral(true).queue();
            return;
        }

        teams.sort(Comparator.comparingInt(t -> t.lp));

        List<Map<String, Object>> round1 = new ArrayList<>();
        for (int i = 0; i < teams.size(); i += 2) {
            if (i + 1 >= teams.size()) break; // skip unpaired team
            Map<String, Object> match = new HashMap<>();
            match.put("team1", teams.get(i).name);
            match.put("team2", teams.get(i + 1).name);
            match.put("winner", null);
            round1.add(match);
        }

        int qfCount = round1.size() / 2;
        List<Map<String, Object>> quarterfinals = new ArrayList<>();
        for (int i = 0; i < qfCount; i++) {
            Map<String, Object> emptyMatch = new HashMap<>();
            emptyMatch.put("team1", null);
            emptyMatch.put("team2", null);
            emptyMatch.put("winner", null);
            quarterfinals.add(emptyMatch);
        }

        List<Map<String, Object>> semifinals = new ArrayList<>();
        Map<String, Object> semi = new HashMap<>();
        semi.put("team1", null);
        semi.put("team2", null);
        semi.put("winner", null);
        semifinals.add(semi);

        Map<String, Object> rounds = new LinkedHashMap<>();
        rounds.put("rounds", List.of(
                Map.of("name", "Round 1", "matches", round1),
                Map.of("name", "Quarterfinals", "matches", quarterfinals),
                Map.of("name", "Semifinals", "matches", semifinals)
        ));

        try {
            File out = new File(ROUNDS_FILE);
            out.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, rounds);
        } catch (IOException e) {
            event.reply("❌ Failed to write tournament rounds file.").setEphemeral(true).queue();
            return;
        }

        StringBuilder reply = new StringBuilder("✅ Tournament bracket created and rounds initialized.");
        if (!skippedTeams.isEmpty()) {
            reply.append("\n⚠️ Skipped teams with fewer than 5 members:\n");
            for (String name : skippedTeams) {
                reply.append("• ").append(name).append("\n");
            }
        }

        StaffLogger.log(event.getJDA(), StaffLogger.LogType.STAFF, "🏁 ``start_tournament`` used by <@" + userId + ">. " +
                teams.size() + " teams included. Skipped: " + skippedTeams.size());
        event.reply(reply.toString()).queue();
    }

    static class TeamData {
        String name;
        int lp;

        TeamData(String name, int lp) {
            this.name = name;
            this.lp = lp;
        }
    }
}