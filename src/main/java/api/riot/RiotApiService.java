package api.riot;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;

public class RiotApiService {
    private static final String API_KEY = "";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    // Step 1: Get PUUID by Riot ID (gameName + tagLine)
    public static JsonObject getAccountByRiotId(String gameName, String tagLine) throws IOException {
        String url = String.format(
                "https://americas.api.riotgames.com/riot/account/v1/accounts/by-riot-id/%s/%s",
                gameName, tagLine
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Riot ID lookup failed");
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
    }

    // Step 2: Get Summoner Info by PUUID
    public static JsonObject getSummonerByPUUID(String region, String puuid) throws IOException {
        String url = String.format(
                "https://%s.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/%s",
                region, puuid
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Summoner lookup failed");
            return gson.fromJson(response.body().string(), JsonObject.class);
        }
    }

    // Step 3: Get Rank by Summoner ID
    public static JsonArray getRankBySummonerId(String region, String summonerId) throws IOException {
        String url = String.format(
                "https://%s.api.riotgames.com/lol/league/v4/entries/by-summoner/%s",
                region, summonerId
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Rank lookup failed");
            return gson.fromJson(response.body().string(), JsonArray.class);
        }
    }

    public static JsonArray getRankedMatchHistory(String matchRegion, String puuid, long afterEpoch) throws IOException {
        String url = String.format(
                "https://%s.api.riotgames.com/lol/match/v5/matches/by-puuid/%s/ids?start=0&count=20&startTime=%d",
                matchRegion, puuid, afterEpoch
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Riot-Token", API_KEY)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Match history fetch failed");
            return gson.fromJson(response.body().string(), JsonArray.class);
        }
    }


}
