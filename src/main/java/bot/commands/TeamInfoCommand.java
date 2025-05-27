package bot.commands;

import api.riot.RiotApiService;
import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.RankCache;

import java.io.File;
import java.io.FileReader;

public class TeamInfoCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("teaminfo")) return;

        String discordId = event.getUser().getId();
        event.deferReply().queue();

        try {
            File[] teamFiles = new File("data/teams").listFiles();
            if (teamFiles == null) {
                event.getHook().sendMessage("❌ No teams found.").queue();
                return;
            }

            for (File file : teamFiles) {
                JsonObject team = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                JsonArray members = team.getAsJsonArray("members");

                for (JsonElement m : members) {
                    JsonObject member = m.getAsJsonObject();
                    if (member.get("discordId").getAsString().equals(discordId)) {
                        // Build team info
                        StringBuilder sb = new StringBuilder();
                        sb.append("**Team Name:** ").append(team.get("teamName").getAsString()).append("\n\n");
                        sb.append("**Members:**\n");

                        for (JsonElement memEl : members) {
                            JsonObject mem = memEl.getAsJsonObject();
                            String ign = mem.get("leagueIGN").getAsString();
                            String puuid = mem.get("leagueUID").getAsString();
                            String discordName = mem.get("discordName").getAsString();
                            String rankDisplay = getSoloQueueRank(puuid);

                            sb.append("- ").append(discordName)
                                    .append(" (").append(ign).append(")")
                                    .append(" - ").append(rankDisplay).append("\n");
                        }

                        event.getHook().sendMessage(sb.toString()).queue();
                        return;
                    }
                }
            }

            event.getHook().sendMessage("❌ You are not currently in a team.").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Failed to retrieve team info.").queue();
        }
    }

    private String getSoloQueueRank(String puuid) {
        try {
            // Check cache first
            String cached = RankCache.get(puuid);
            if (cached != null) return cached;

            // Not cached, fetch from Riot
            JsonObject summoner = RiotApiService.getSummonerByPUUID("na1", puuid);
            String summonerId = summoner.get("id").getAsString();
            JsonArray ranks = RiotApiService.getRankBySummonerId("na1", summonerId);

            for (JsonElement el : ranks) {
                JsonObject entry = el.getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(entry.get("queueType").getAsString())) {
                    String result = entry.get("tier").getAsString() + " " +
                            entry.get("rank").getAsString() + " (" +
                            entry.get("leaguePoints").getAsInt() + " LP)";
                    RankCache.put(puuid, result);
                    return result;
                }
            }

            RankCache.put(puuid, "Unranked");
            return "Unranked";
        } catch (Exception e) {
            return "Unranked or Error";
        }
    }

}
