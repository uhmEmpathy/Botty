package bot.commands;

import api.riot.RiotApiService;
import util.RankCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CheckRankCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("checkrank")) return;

        String riotId = event.getOption("ign").getAsString().trim();
        event.deferReply().queue();

        try {
            String[] parts = riotId.split("#");
            if (parts.length != 2) {
                event.getHook().sendMessage("❌ Invalid Riot ID format. Use `GameName#TagLine` (e.g., Empathy#NA3)").queue();
                return;
            }

            String gameName = parts[0];
            String tagLine = parts[1];

            JsonObject account = RiotApiService.getAccountByRiotId(gameName, tagLine);
            String puuid = account.get("puuid").getAsString();

            // ✅ Check cache first
            String cached = RankCache.get(puuid);
            if (cached != null) {
                event.getHook().sendMessage("**" + riotId + "** is currently **" + cached + "** in Solo Queue.").queue();
                return;
            }

            // Not cached → fetch fresh
            JsonObject summoner = RiotApiService.getSummonerByPUUID("na1", puuid);
            String summonerId = summoner.get("id").getAsString();
            JsonArray ranks = RiotApiService.getRankBySummonerId("na1", summonerId);

            for (int i = 0; i < ranks.size(); i++) {
                JsonObject entry = ranks.get(i).getAsJsonObject();
                if ("RANKED_SOLO_5x5".equals(entry.get("queueType").getAsString())) {
                    String tier = entry.get("tier").getAsString();
                    String rank = entry.get("rank").getAsString();
                    int lp = entry.get("leaguePoints").getAsInt();
                    String display = tier + " " + rank + " (" + lp + " LP)";
                    RankCache.put(puuid, display);

                    event.getHook().sendMessage("**" + riotId + "** is currently **" + display + "** in Solo Queue.").queue();
                    return;
                }
            }

            RankCache.put(puuid, "Unranked");
            event.getHook().sendMessage("No Solo Queue rank found for `" + riotId + "`").queue();

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Error fetching rank for `" + riotId + "`").queue();
        }
    }
}
