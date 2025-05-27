package util;

import com.google.gson.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RankCache {
    private static final File cacheFile = new File("data/cache/rank_cache.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final long CACHE_DURATION = 15 * 60 * 1000; // 15 minutes

    private static Map<String, CachedRank> cache = new HashMap<>();

    static {
        loadCache();
    }

    public static String get(String puuid) {
        CachedRank entry = cache.get(puuid);
        if (entry != null && System.currentTimeMillis() < entry.expires) {
            return entry.rank;
        }
        return null;
    }

    public static void put(String puuid, String rank) {
        cache.put(puuid, new CachedRank(rank, System.currentTimeMillis() + CACHE_DURATION));
        saveCache();
    }

    private static void loadCache() {
        try {
            if (!cacheFile.exists()) {
                cacheFile.getParentFile().mkdirs();
                cacheFile.createNewFile();
                saveCache();
                return;
            }
            JsonObject raw = JsonParser.parseReader(new FileReader(cacheFile)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
                JsonObject obj = entry.getValue().getAsJsonObject();
                cache.put(entry.getKey(), new CachedRank(
                        obj.get("rank").getAsString(),
                        obj.get("expires").getAsLong()
                ));
            }
        } catch (Exception ignored) {}
    }

    private static void saveCache() {
        try (FileWriter writer = new FileWriter(cacheFile)) {
            JsonObject raw = new JsonObject();
            for (Map.Entry<String, CachedRank> entry : cache.entrySet()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("rank", entry.getValue().rank);
                obj.addProperty("expires", entry.getValue().expires);
                raw.add(entry.getKey(), obj);
            }
            gson.toJson(raw, writer);
        } catch (Exception ignored) {}
    }

    private static class CachedRank {
        String rank;
        long expires;

        CachedRank(String rank, long expires) {
            this.rank = rank;
            this.expires = expires;
        }
    }
}
