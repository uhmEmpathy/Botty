
package bot.commands;

import api.riot.RiotApiService;
import com.google.gson.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class TrackerCommand extends ListenerAdapter {

    private static final String TRACK_NAME = "gorillajones";
    private static final String TRACK_TAG = "FIGHT";
    private static final String REGION = "na1";
    private static final String MATCH_REGION = "americas";
    private static final String TRACKING_FOLDER = "data/tracker/gorillajones/";
    private static final ZoneId EST = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("tracker")) return;
        event.deferReply().queue();

        try {
            JsonObject account = RiotApiService.getAccountByRiotId(TRACK_NAME, TRACK_TAG);
            String puuid = account.get("puuid").getAsString();

            JsonObject summoner = RiotApiService.getSummonerByPUUID(REGION, puuid);
            String summonerId = summoner.get("id").getAsString();

            JsonArray rankArray = RiotApiService.getRankBySummonerId(REGION, summonerId);
            JsonObject soloQueue = null;
            for (JsonElement el : rankArray) {
                JsonObject rank = el.getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(rank.get("queueType").getAsString())) {
                    soloQueue = rank;
                    break;
                }
            }

            if (soloQueue == null) {
                event.getHook().editOriginal("❌ No ranked solo data found.").queue();
                return;
            }

            String tier = soloQueue.get("tier").getAsString();
            String division = soloQueue.get("rank").getAsString();
            int currentLp = soloQueue.get("leaguePoints").getAsInt();
            System.out.println("🔄 Live LP from Riot: " + currentLp);

            // Determine snapshot for today (resets at 2:45 AM EST)
            LocalDate today = ZonedDateTime.now(EST).withHour(2).withMinute(45).withSecond(0).withNano(0).toLocalDate();
            String filename = TRACKING_FOLDER + today.format(DATE_FORMAT) + ".json";

            File file = new File(filename);
            File dir = new File(TRACKING_FOLDER);
            if (!dir.exists()) dir.mkdirs();

            int startingLp = currentLp;
            Set<String> recordedMatches = new HashSet<>();

            JsonObject fileData;
            if (file.exists()) {
                fileData = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                startingLp = fileData.get("startingLp").getAsInt();
                for (JsonElement id : fileData.getAsJsonArray("matches")) {
                    recordedMatches.add(id.getAsString());
                }
            } else {
                fileData = new JsonObject();
                fileData.addProperty("startingLp", currentLp);
                fileData.add("matches", new JsonArray());
                try (FileWriter writer = new FileWriter(file)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(fileData, writer);
                }
            }

            int lpDiff = currentLp - startingLp;

            // Match tracking
            long sinceEpoch = ZonedDateTime.of(today, LocalTime.of(2, 45), EST).toEpochSecond();
            JsonArray matches = RiotApiService.getRankedMatchHistory(MATCH_REGION, puuid, sinceEpoch);

            JsonArray matchArray = fileData.getAsJsonArray("matches");
            for (JsonElement matchId : matches) {
                String match = matchId.getAsString();
                if (!recordedMatches.contains(match)) {
                    matchArray.add(match);
                }
            }

            // Save updated match list
            fileData.add("matches", matchArray);
            try (FileWriter writer = new FileWriter(file)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(fileData, writer);
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("📊 Tracker - gorillajones#FIGHT");
            embed.setColor(Color.GREEN);
            embed.setDescription(String.format(
                    "**Rank:** %s %s\n**Current LP:** %d\n**Starting LP Today:** %d\n**LP Gained/Lost Today:** %s%d LP\n**Games Played Today:** %d",
                    tier, division, currentLp, startingLp,
                    lpDiff >= 0 ? "+" : "", lpDiff, matchArray.size()
            ));
            embed.setFooter("Resets daily at 2:45 AM EST");

            event.getHook().editOriginalEmbeds(embed.build()).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().editOriginal("❌ Error fetching tracker data: " + e.getMessage()).queue();
        }
    }
}
