
package bot.commands;

import api.riot.RiotApiService;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class RegisterCommand extends ListenerAdapter {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String VERIFICATION_FILE = "data/verification/pending-verification.json";
    private static final List<Integer> VALID_ICON_IDS = Arrays.asList(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
            10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28
    );
    private static final String DDRAGON_VERSION = "14.9.1";

    private static class PendingUser {
        String ign;
        String tag;
        int iconId;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("register")) return;

        String riotId = event.getOption("ign").getAsString().trim();
        String discordId = event.getUser().getId();

        event.deferReply().queue();

        try {
            String[] parts = riotId.split("#");
            if (parts.length != 2) {
                event.getHook().sendMessage("❌ Invalid Riot ID format. Use IGN#REGION").queue();
                return;
            }

            String ign = parts[0];
            String tag = parts[1];

            JsonObject summoner = RiotApiService.getAccountByRiotId(ign, tag);
            if (summoner == null) {
                event.getHook().sendMessage("❌ Could not find League account with that Riot ID.").queue();
                return;
            }

            String puuid = summoner.get("puuid").getAsString();
            JsonObject profile = RiotApiService.getSummonerByPUUID("na1", puuid);
            int currentIconId = profile.get("profileIconId").getAsInt();

            Map<String, PendingUser> pendingMap = loadPendingMap();

            if (pendingMap.containsKey(discordId)) {
                PendingUser existing = pendingMap.get(discordId);
                String iconUrl = "https://ddragon.leagueoflegends.com/cdn/" + DDRAGON_VERSION + "/img/profileicon/" + existing.iconId + ".png";

                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Summoner Icon Verification");
                embed.setDescription("You're already pending verification. Change your summoner icon to the one below and run `/verify`.");
                embed.setImage(iconUrl);
                embed.setThumbnail(iconUrl);
                embed.setFooter("Icon ID: " + existing.iconId);

                event.getHook().sendMessageEmbeds(embed.build()).queue();
                return;
            }

            List<Integer> availableIcons = new ArrayList<>(VALID_ICON_IDS);
            availableIcons.removeIf(id -> id == currentIconId);

            if (availableIcons.isEmpty()) {
                event.getHook().sendMessage("⚠️ Could not assign a verification icon. Try changing your current icon and run `/register` again.").queue();
                return;
            }

            int assignedIcon = availableIcons.get(new Random().nextInt(availableIcons.size()));
            PendingUser pending = new PendingUser();
            pending.ign = ign;
            pending.tag = tag;
            pending.iconId = assignedIcon;

            pendingMap.put(discordId, pending);
            savePendingMap(pendingMap);

            String iconUrl = "https://ddragon.leagueoflegends.com/cdn/" + DDRAGON_VERSION + "/img/profileicon/" + assignedIcon + ".png";

            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Summoner Icon Verification");
            embed.setDescription("🛡️ To verify your League account: "
                            + "1. Change your summoner icon to the one below "
                            + "2. Then run `/verify` to complete registration.");
            embed.setImage(iconUrl);
            embed.setThumbnail(iconUrl);
            embed.setFooter("Icon ID: " + assignedIcon);

            event.getHook().sendMessageEmbeds(embed.build()).queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Something went wrong during registration.").queue();
        }
    }

    private Map<String, PendingUser> loadPendingMap() {
        try {
            File file = new File(VERIFICATION_FILE);
            if (!file.exists()) return new HashMap<>();
            try (FileReader reader = new FileReader(file)) {
                return gson.fromJson(reader, new TypeToken<Map<String, PendingUser>>() {}.getType());
            }
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void savePendingMap(Map<String, PendingUser> map) {
        try {
            File file = new File(VERIFICATION_FILE);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                gson.toJson(map, writer);
            }
        } catch (Exception ignored) {}
    }
}
