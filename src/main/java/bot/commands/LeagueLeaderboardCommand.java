
package bot.commands;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import api.riot.RiotApiService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "data/players/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("league-leaderboard")) return;
        EmbedBuilder embed = buildLeaderboardEmbed();
        event.replyEmbeds(embed.build())
                .addActionRow(Button.primary("refresh_leaderboard", "🔄 Refresh"))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("refresh_leaderboard")) return;
        EmbedBuilder embed = buildLeaderboardEmbed();
        event.deferEdit().queue();
        event.getHook().editOriginalEmbeds(embed.build()).queue();
    }

    private EmbedBuilder buildLeaderboardEmbed() {
        List<String> entries = new ArrayList<>();

        File dir = new File(PLAYER_FOLDER);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) files = new File[0];

        for (File file : files) {
            try {
                JsonObject obj = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                String discordName = obj.has("discordName") ? obj.get("discordName").getAsString().split("#")[0] : file.getName();
                String leagueIGN = obj.get("leagueIGN").getAsString();
                String leagueUID = obj.get("leagueUID").getAsString();

                JsonObject summoner = RiotApiService.getSummonerByPUUID("na1", leagueUID);
                String summonerId = summoner.get("id").getAsString();
                JsonArray ranks = RiotApiService.getRankBySummonerId("na1", summonerId);

                String tier = "UNRANKED", division = "";
                int lp = 0, wins = 0, losses = 0;

                for (JsonElement rankEl : ranks) {
                    JsonObject rank = rankEl.getAsJsonObject();
                    if ("RANKED_SOLO_5x5".equals(rank.get("queueType").getAsString())) {
                        tier = rank.get("tier").getAsString();
                        division = rank.get("rank").getAsString();
                        lp = rank.get("leaguePoints").getAsInt();
                        wins = rank.get("wins").getAsInt();
                        losses = rank.get("losses").getAsInt();
                        break;
                    }
                }

                obj.addProperty("rankTier", tier);
                obj.addProperty("rankDivision", division);
                obj.addProperty("leaguePoints", lp);
                try (FileWriter writer = new FileWriter(file)) {
                    new GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
                }

                int totalGames = wins + losses;
                String winRate = totalGames > 0 ? Math.round((100.0 * wins / totalGames)) + "%" : "0%";
                String encodedIgn = leagueIGN.replace(" ", "%20").replace("#", "-");
                String opggLink = String.format("[%s](https://www.op.gg/summoners/na/%s)", leagueIGN, encodedIgn);
                String rankStr = formatRankWithEmoji(tier, division, lp);

                entries.add(String.format("%s - %s - %s | %dW %dL (%s)", discordName, opggLink, rankStr, wins, losses, winRate));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        entries.sort((a, b) -> {
            try {
                String[] partsA = a.split(" - ");
                String[] partsB = b.split(" - ");
                //String rankA = partsA[2].split(" \| ")[0].replaceAll("<:[^>]+>", "").trim();
                //String rankB = partsB[2].split(" \| ")[0].replaceAll("<:[^>]+>", "").trim();
                String rankA = partsA[2].split(" \\| ")[0].replaceAll("<:[^>]+>", "").trim();
                String rankB = partsB[2].split(" \\| ")[0].replaceAll("<:[^>]+>", "").trim();
                String[] rankPartsA = rankA.split(" ");
                String[] rankPartsB = rankB.split(" ");
                String tierA = rankPartsA[0].toUpperCase();
                String divA = rankPartsA.length > 1 ? rankPartsA[1] : "I";
                String tierB = rankPartsB[0].toUpperCase();
                String divB = rankPartsB.length > 1 ? rankPartsB[1] : "I";

                int lpA = extractLP(rankA), lpB = extractLP(rankB);
                int tierValA = tierToValue(tierA), tierValB = tierToValue(tierB);
                int divValA = divisionToValue(divA), divValB = divisionToValue(divB);

                if (tierValA != tierValB) return Integer.compare(tierValB, tierValA);
                if (divValA != divValB) return Integer.compare(divValB, divValA);
                return Integer.compare(lpB, lpA);
            } catch (Exception e) {
                return 0;
            }
        });

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            content.append(String.format("%d. %s\n", i + 1, entries.get(i)));
        }

        try {
            String fileName = "data/lp_tracking/" + LocalDate.now(ZoneId.of("America/New_York")) + ".json";
            File file = new File(fileName);
            if (file.exists()) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, Integer>>() {}.getType();
                Map<String, Integer> lpChanges = gson.fromJson(new FileReader(file), type);

                String maxGainId = null, maxLossId = null;
                int maxGain = Integer.MIN_VALUE, maxLoss = Integer.MAX_VALUE;

                for (Map.Entry<String, Integer> entry : lpChanges.entrySet()) {
                    int delta = entry.getValue();
                    if (delta > maxGain) { maxGain = delta; maxGainId = entry.getKey(); }
                    if (delta < maxLoss) { maxLoss = delta; maxLossId = entry.getKey(); }
                }

                if (maxGainId != null && maxGain > 0) {
                    final String id = maxGainId;
                    File[] match = new File("data/players").listFiles((f, name) -> name.startsWith(id + " -"));
                    if (match != null && match.length > 0) {
                        JsonObject data = gson.fromJson(new FileReader(match[0]), JsonObject.class);
                        content.append("\n\n🔼 **Highest LP Gained Today:** ")
                                .append(data.get("discordName").getAsString())
                                .append(" (+").append(maxGain).append(" LP)");
                    }
                } else {
                    content.append("\n\n🔼 **Highest LP Gained Today:** No players gained LP today.");
                }

                if (maxLossId != null && maxLoss < 0) {
                    final String id = maxLossId;
                    File[] match = new File("data/players").listFiles((f, name) -> name.startsWith(id + " -"));
                    if (match != null && match.length > 0) {
                        JsonObject data = gson.fromJson(new FileReader(match[0]), JsonObject.class);
                        content.append("\n🔽 **Highest LP Lost Today:** ")
                                .append(data.get("discordName").getAsString())
                                .append(" (").append(maxLoss).append(" LP)");
                    }
                } else {
                    content.append("\n🔽 **Highest LP Lost Today:** No players lost LP today.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new EmbedBuilder()
                .setTitle("🏆 League Rank Leaderboard")
                .setColor(Color.CYAN)
                .setDescription(content.toString())
                .setFooter("Last updated: " + LocalDate.now(ZoneId.of("America/New_York")));
    }

    private String formatRankWithEmoji(String tier, String division, int lp) {
        String emoji = switch (tier.toUpperCase()) {
            case "CHALLENGER" -> "<:challenger:1362148639191072920> ";
            case "GRANDMASTER" -> "<:grandmaster:1362148647302598927> ";
            case "MASTER" -> "<:master:1362148641166459150> ";
            case "DIAMOND" -> "<:diamond:1362148656685252638> ";
            case "EMERALD" -> "<:emerald:1362165186475327558> ";
            case "PLATINUM" -> "<:platinum:1362148643087319110> ";
            case "GOLD" -> "<:gold:1362148654949073017> ";
            case "SILVER" -> "<:silver:1362148652344279200> ";
            case "BRONZE" -> "<:bronze:1362148645062971402> ";
            case "IRON" -> "<:iron:1362148649508798687> ";
            default -> "<:default:1376672985989251154> ";
        };
        return emoji + tier + " " + division + " (" + lp + " LP)";
    }

    private int extractLP(String rankStr) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\((\\d+) LP\\)").matcher(rankStr);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));
        return 0;
    }

    private int divisionToValue(String div) {
        return switch (div.toUpperCase()) {
            case "I" -> 4;
            case "II" -> 3;
            case "III" -> 2;
            case "IV" -> 1;
            default -> 0;
        };
    }

    private int tierToValue(String tier) {
        return switch (tier) {
            case "CHALLENGER" -> 9;
            case "GRANDMASTER" -> 8;
            case "MASTER" -> 7;
            case "DIAMOND" -> 6;
            case "EMERALD" -> 5;
            case "PLATINUM" -> 4;
            case "GOLD" -> 3;
            case "SILVER" -> 2;
            case "BRONZE" -> 1;
            default -> 0;
        };
    }
}
