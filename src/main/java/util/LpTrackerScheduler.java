
package util;

import api.riot.RiotApiService;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class LpTrackerScheduler {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String PLAYER_DIR = "data/players/";
    private static final String TRACKING_DIR = "data/lp_tracking/";
    private static final ZoneId ZONE_ID = ZoneId.of("America/New_York");
    private static final int RESET_HOUR = 15; // 3 PM EST

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void start() {
        Runnable task = () -> {
            try {
                trackLPChanges();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.MINUTES);
    }

    private static void trackLPChanges() throws Exception {
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalTime now = LocalTime.now(ZONE_ID);
        String fileName = TRACKING_DIR + today.format(DateTimeFormatter.ISO_DATE) + ".json";

        File trackFile = new File(fileName);
        if (now.getHour() == RESET_HOUR && now.getMinute() < 10 && trackFile.exists()) {
            trackFile.delete();
        }

        Map<String, Integer> lpChanges = trackFile.exists() ? loadJson(fileName) : new HashMap<>();

        File playerFolder = new File(PLAYER_DIR);
        if (!playerFolder.exists()) return;

        for (File file : Objects.requireNonNull(playerFolder.listFiles())) {
            JsonObject player = gson.fromJson(new FileReader(file), JsonObject.class);
            String discordId = player.get("discordId").getAsString();
            String puuid = player.get("leagueUID").getAsString();
            String region = "na1"; // Or dynamically determine based on user setup
            JsonObject profile = RiotApiService.getSummonerByPUUID(region, puuid);
            String summonerId = profile.get("id").getAsString();


            JsonArray rankArray;
            try {
                rankArray = RiotApiService.getRankBySummonerId(region, summonerId);
            } catch (IOException e) {
                System.err.println("⚠️ Failed to fetch rank for " + discordId + ": " + e.getMessage());
                continue;
            }
            JsonObject solo = null;
            for (JsonElement e : rankArray) {
                JsonObject r = e.getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(r.get("queueType").getAsString())) {
                    solo = r;
                    break;
                }
            }

            if (solo != null) {
                int currentLP = solo.get("leaguePoints").getAsInt();
                String currentTier = solo.get("tier").getAsString();
                String currentDiv = solo.get("rank").getAsString();

                int oldLP = player.has("leaguePoints") ? player.get("leaguePoints").getAsInt() : -1;
                String oldTier = player.has("rankTier") ? player.get("rankTier").getAsString() : "";
                String oldDiv = player.has("rankDivision") ? player.get("rankDivision").getAsString() : "";

                if (oldLP != currentLP || !oldTier.equals(currentTier) || !oldDiv.equals(currentDiv)) {
                    player.addProperty("rankTier", currentTier);
                    player.addProperty("rankDivision", currentDiv);
                    player.addProperty("leaguePoints", currentLP);

                    try (FileWriter writer = new FileWriter(file)) {
                        gson.toJson(player, writer);
                    }

                    if (oldLP != -1) {
                        int delta = currentLP - oldLP;
                        lpChanges.put(discordId, lpChanges.getOrDefault(discordId, 0) + delta);
                        System.out.printf("🔄 Updated LP for %s: %d → %d (%+d LP)%n", discordId, oldLP, currentLP, delta);
                    }
                }
            }
        }

        File dir = new File(TRACKING_DIR);
        if (!dir.exists()) dir.mkdirs();

        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(lpChanges, writer);
        }
    }

    private static Map<String, Integer> loadJson(String path) {
        try (FileReader reader = new FileReader(path)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            return gson.fromJson(reader, type);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
