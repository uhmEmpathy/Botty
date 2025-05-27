package bot.commands;

import com.google.gson.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileReader;
import java.util.*;

public class InhouseLadderCommand extends ListenerAdapter {

    private static final String PLAYER_FOLDER = "data/players/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("inhouse-ladder")) return;

        File dir = new File(PLAYER_FOLDER);
        if (!dir.exists() || !dir.isDirectory()) {
            event.reply("❌ Player data folder not found.").setEphemeral(true).queue();
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            event.reply("📭 No registered players found.").setEphemeral(true).queue();
            return;
        }

        List<String> ladder = new ArrayList<>();

        for (File file : files) {
            try {
                JsonObject obj = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                String discordName = obj.has("discordName") ? obj.get("discordName").getAsString() : file.getName();
                int wins = obj.has("inhouseWins") ? obj.get("inhouseWins").getAsInt() : 0;
                int losses = obj.has("inhouseLosses") ? obj.get("inhouseLosses").getAsInt() : 0;
                int total = wins + losses;
                String winRate = total > 0 ? String.format("%.1f%%", (100.0 * wins / total)) : "0.0%";
                ladder.add(String.format("%s — %dW / %dL (%s)", discordName, wins, losses, winRate));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ladder.sort((a, b) -> {
            int aWins = Integer.parseInt(a.split("—")[1].split("W")[0].trim());
            int bWins = Integer.parseInt(b.split("—")[1].split("W")[0].trim());
            return Integer.compare(bWins, aWins);
        });

        StringBuilder output = new StringBuilder("📈 **Inhouse Ladder:**\n\n");
        for (int i = 0; i < ladder.size(); i++) {
            output.append(i + 1).append(". ").append(ladder.get(i)).append("\n");
        }

        event.reply(output.toString()).queue();
    }
}