package data;

import data.models.Player;
import data.models.Team;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class DataManager {
    private static final File file = new File("src/bot/storage/tournament.json");

    public static Map<String, Player> players = new HashMap<>();
    public static Map<String, Team> teams = new HashMap<>();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void load() {
        if (!file.exists()) {
            save(); // Create empty file if not exists
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);

            Type playerMapType = new TypeToken<Map<String, Player>>() {}.getType();
            Type teamMapType = new TypeToken<Map<String, Team>>() {}.getType();

            players = gson.fromJson(root.get("players"), playerMapType);
            teams = gson.fromJson(root.get("teams"), teamMapType);

        } catch (Exception e) {
            System.err.println("Error loading tournament data: " + e.getMessage());
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(file)) {
            JsonObject root = new JsonObject();
            root.add("players", gson.toJsonTree(players));
            root.add("teams", gson.toJsonTree(teams));
            gson.toJson(root, writer);
        } catch (Exception e) {
            System.err.println("Error saving tournament data: " + e.getMessage());
        }
    }
}
