package bot.commands;

import api.riot.RiotApiService;
import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AutoCreateTeamsCommand extends ListenerAdapter {

    private static final String PLAYER_DATA_FILE = "data/auto_teams/enriched_players.json";
    private static final String TEAM_FILE_DIR = "data/auto_teams/generated_teams/";
    private static final int TEAM_LIMIT = 8;

    private static final String[] RIOT_IDS = {
            "Freshy#cat", "Empathy#NA3", "Empathy#NAッ", "Crucible#Geto", "어느 화창한 여름#NA01",
            "Xan1924#NA1", "C9 Loki#kr3", "kisno#NA1", "Daption#TwTv", "알리페데#사도조한",
            "Evei#tudi", "TvAnUglyBroccoli#TTV", "Tychee#1437", "Kanami#ahj", "Cendi#NA1"
    };

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("create_teams_from_file")) return;

        File teamDir = new File(TEAM_FILE_DIR);
        if (teamDir.exists() && teamDir.isDirectory()) {
            File[] existingTeams = teamDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (existingTeams != null && existingTeams.length >= TEAM_LIMIT) {
                event.reply("❌ There are already " + existingTeams.length + " auto-generated teams. Remove them before generating new ones.").setEphemeral(true).queue();
                return;
            }
        }

        event.deferReply().queue();

        List<JsonObject> players = new ArrayList<>();
        List<String> skippedIds = new ArrayList<>();
        int tagCounter = 0;

        for (String riotId : RIOT_IDS) {
            try {
                String[] parts = riotId.split("#");
                if (parts.length != 2) continue;
                String gameName = parts[0];
                String tagLine = parts[1];

                JsonObject account;
                try {
                    account = RiotApiService.getAccountByRiotId(gameName, tagLine);
                } catch (IOException e) {
                    System.err.println("⚠️ Riot ID lookup failed for " + riotId + ": " + e.getMessage());
                    skippedIds.add(riotId);
                    continue;
                }

                String puuid = account.get("puuid").getAsString();
                JsonObject summoner = RiotApiService.getSummonerByPUUID("na1", puuid);
                String summonerId = summoner.get("id").getAsString();
                JsonArray ranks = RiotApiService.getRankBySummonerId("na1", summonerId);

                String tier = "UNRANKED";
                String division = "";
                int lp = 0;

                for (JsonElement entry : ranks) {
                    JsonObject rank = entry.getAsJsonObject();
                    if ("RANKED_SOLO_5x5".equals(rank.get("queueType").getAsString())) {
                        tier = rank.get("tier").getAsString();
                        division = rank.get("rank").getAsString();
                        lp = rank.get("leaguePoints").getAsInt();
                        break;
                    }
                }

                JsonObject player = new JsonObject();
                player.addProperty("discordName", "Autogen#" + String.format("%04d", tagCounter));
                player.addProperty("discordId", "autogen-" + String.format("%04d", tagCounter));
                player.addProperty("leagueIGN", riotId);
                player.addProperty("leagueUID", puuid);
                player.addProperty("rankTier", tier);
                player.addProperty("rankDivision", division);
                player.addProperty("leaguePoints", lp);

                players.add(player);
                tagCounter++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        saveToJson(players);

        int playersNeeded = TEAM_LIMIT * 5;

        List<JsonObject> fullRoster = new ArrayList<>();
        while (fullRoster.size() < playersNeeded) {
            fullRoster.addAll(players);
        }
        Collections.shuffle(fullRoster);
        fullRoster = fullRoster.subList(0, playersNeeded);

        List<List<JsonObject>> teams = new ArrayList<>();
        for (int i = 0; i < TEAM_LIMIT; i++) {
            teams.add(new ArrayList<>(fullRoster.subList(i * 5, (i + 1) * 5)));
        }

        List<String> teamNames = generateRandomTeamNames(TEAM_LIMIT);
        saveTeamsToJson(teams, teamNames);
        event.getHook().sendMessage("✅ Teams created using Riot IDs. Skipped: " + skippedIds.size()).queue();
    }

    private void saveToJson(List<JsonObject> players) {
        File dir = new File("data/auto_teams");
        if (!dir.exists()) dir.mkdirs();

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try (FileWriter writer = new FileWriter(PLAYER_DATA_FILE)) {
            gson.toJson(players, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> generateRandomTeamNames(int count) {
        List<String> teamNames = new ArrayList<>();
        Random random = new Random();

        String[] adjectives = {"Mighty", "Flying", "Thunder", "Wild", "Stealthy", "Furious", "Iron", "Epic", "Shadow", "Blazing"};
        String[] nouns = {"Wolves", "Dragons", "Falcons", "Titans", "Lions", "Eagles", "Tigers", "Phoenix", "Bears", "Sharks"};

        while (teamNames.size() < count) {
            String name = adjectives[random.nextInt(adjectives.length)] + " " + nouns[random.nextInt(nouns.length)];
            if (name.length() > 12) continue;
            if (!teamNames.contains(name)) teamNames.add(name);
        }

        return teamNames;
    }

    private void saveTeamsToJson(List<List<JsonObject>> teams, List<String> teamNames) {
        File teamFileDir = new File(TEAM_FILE_DIR);
        if (!teamFileDir.exists()) {
            teamFileDir.mkdirs();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        for (int i = 0; i < teams.size(); i++) {
            String teamName = teamNames.get(i);
            List<JsonObject> teamMembers = teams.get(i);
            File teamFile = new File(TEAM_FILE_DIR + teamName.replace(" ", "_") + ".json");

            JsonObject teamObject = new JsonObject();
            teamObject.addProperty("teamName", teamName);
            JsonArray membersArray = new JsonArray();

            for (JsonObject player : teamMembers) {
                membersArray.add(player);
            }

            teamObject.add("members", membersArray);

            try (FileWriter writer = new FileWriter(teamFile)) {
                gson.toJson(teamObject, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}