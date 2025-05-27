package bot.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.io.File;
import java.util.*;

public class CreateTeamCommand extends ListenerAdapter {
    private static final String TEAMS_FOLDER = "data/teams/";
    private static final String STAFF_FILE = "data/staff/staff.json";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("create_team")) return;

        String userId = event.getUser().getId();
        OptionMapping option = event.getOption("name");

        if (option == null) {
            event.reply("❌ You must provide a team name.").setEphemeral(true).queue();
            return;
        }

        String teamName = option.getAsString().trim();

        if (teamName.length() > 12) {
            event.reply("❌ Team name must not exceed 12 characters.").setEphemeral(true).queue();
            return;
        }

        File[] files = new File(TEAMS_FOLDER).listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equalsIgnoreCase(teamName + ".json")) {
                    event.reply("❌ A team with that name already exists.").setEphemeral(true).queue();
                    return;
                }
            }
        }

        File userFile = new File("data/players/" + userId + ".json");
        if (!userFile.exists()) {
            event.reply("❌ You must be registered to create a team.").setEphemeral(true).queue();
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> playerData = mapper.readValue(userFile, Map.class);

            Map<String, Object> teamData = new LinkedHashMap<>();
            teamData.put("teamName", teamName);
            teamData.put("members", List.of(playerData));

            File out = new File(TEAMS_FOLDER + teamName + ".json");
            out.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, teamData);

            event.reply("✅ Team `" + teamName + "` has been created.").queue();

        } catch (Exception e) {
            event.reply("❌ Failed to create the team.").setEphemeral(true).queue();
        }
    }
}
