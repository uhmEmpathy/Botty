
package bot.commands;

import api.riot.RiotApiService;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class VerifyCommand extends ListenerAdapter {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String VERIFICATION_FILE = "data/verification/pending-verification.json";
    private static final String PLAYER_FOLDER = "data/players/";

    private static final Map<String, String> RANK_ROLE_IDS = Map.ofEntries(
            Map.entry("CHALLENGER", "1104083066193977425"),
            Map.entry("GRANDMASTER", "1095503610558808218"),
            Map.entry("MASTER", "1095503452618100847"),
            Map.entry("DIAMOND", "1095503295197479115"),
            Map.entry("EMERALD", "1358313265729634417"),
            Map.entry("PLATINUM", "1095503100732784792"),
            Map.entry("GOLD", "1095502968071127051"),
            Map.entry("SILVER", "1095502856653635686"),
            Map.entry("BRONZE", "1095502516449460224"),
            Map.entry("IRON", "1095099153039773767")
    );

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("verify")) return;

        String discordId = event.getUser().getId();
        String discordTag = event.getUser().getAsTag();

        event.deferReply().queue();

        try {
            Map<String, PendingUser> pendingMap = loadPendingMap();

            if (!pendingMap.containsKey(discordId)) {
                event.getHook().sendMessage("❌ You're not pending verification. Use `/register` first.").queue();
                return;
            }

            PendingUser pending = pendingMap.get(discordId);

            JsonObject summoner = RiotApiService.getAccountByRiotId(pending.ign, pending.tag);
            if (summoner == null) {
                event.getHook().sendMessage("❌ Could not find your League account anymore.").queue();
                return;
            }

            String puuid = summoner.get("puuid").getAsString();
            JsonObject profile = RiotApiService.getSummonerByPUUID("na1", puuid);
            int currentIcon = profile.get("profileIconId").getAsInt();

            if (currentIcon != pending.iconId) {
                event.getHook().sendMessage("❌ Your current icon (ID " + currentIcon +
                        ") doesn't match the expected one (ID " + pending.iconId + "). Please change your icon and try again.").queue();
                return;
            }

            JsonObject playerData = new JsonObject();
            playerData.addProperty("discordName", discordTag);
            playerData.addProperty("discordId", discordId);
            playerData.addProperty("leagueIGN", pending.ign + "#" + pending.tag);
            playerData.addProperty("leagueUID", puuid);
            playerData.addProperty("inhouseWins", 0);
            playerData.addProperty("inhouseLosses", 0);

            JsonArray rankArray = RiotApiService.getRankBySummonerId("na1", profile.get("id").getAsString());
            JsonObject soloQueueRank = null;

            for (JsonElement el : rankArray) {
                JsonObject entry = el.getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(entry.get("queueType").getAsString())) {
                    soloQueueRank = entry;
                    break;
                }
            }

            String rankTier = "UNRANKED";
            String rankDivision = "";
            int lp = 0;

            if (soloQueueRank != null) {
                rankTier = soloQueueRank.get("tier").getAsString();
                rankDivision = soloQueueRank.get("rank").getAsString();
                lp = soloQueueRank.get("leaguePoints").getAsInt();
            }

            playerData.addProperty("rankTier", rankTier);
            playerData.addProperty("rankDivision", rankDivision);
            playerData.addProperty("leaguePoints", lp);

            File playerFile = new File(PLAYER_FOLDER + discordId + " - " + discordTag + ".json");
            try (FileWriter writer = new FileWriter(playerFile)) {
                gson.toJson(playerData, writer);
            }

            // Assign Discord rank role
            Guild guild = event.getGuild();
            Member member = guild.getMemberById(discordId);

            if (guild != null && member != null && RANK_ROLE_IDS.containsKey(rankTier)) {
                String roleId = RANK_ROLE_IDS.get(rankTier);
                Role roleToAdd = guild.getRoleById(roleId);

                if (roleToAdd != null) {
                    // Remove other rank roles
                    for (String id : RANK_ROLE_IDS.values()) {
                        Role r = guild.getRoleById(id);
                        if (r != null && member.getRoles().contains(r)) {
                            guild.removeRoleFromMember(member, r).queue();
                        }
                    }
                    // Add new role
                    guild.addRoleToMember(member, roleToAdd).queue();
                }
            }

            pendingMap.remove(discordId);
            savePendingMap(pendingMap);

            event.getHook().sendMessage("✅ Verification successful! You're now registered as **" + pending.ign + "**.").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Something went wrong during verification.").queue();
        }
    }

    private static class PendingUser {
        String ign;
        String tag;
        int iconId;
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
