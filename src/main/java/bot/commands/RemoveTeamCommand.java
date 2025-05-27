package bot.commands;

import util.StaffLogger;
import util.RoleValidator;
//import util.enums.LogType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;

public class RemoveTeamCommand extends ListenerAdapter {
    private final JDA jda;

    public RemoveTeamCommand(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("remove_team")) return;

        String discordId = event.getUser().getId();
        String discordTag = event.getUser().getAsTag();
        String teamName = event.getOption("name").getAsString().trim().toLowerCase();
        File teamFile = new File("data/teams/" + teamName + ".json");

        event.deferReply().queue();

        if (!RoleValidator.isStaffOrAdmin(discordId)) {
            event.getHook().sendMessage("❌ You do not have permission to use this command.").queue();
            return;
        }

        if (!teamFile.exists()) {
            event.getHook().sendMessage("❌ Team `" + teamName + "` does not exist.").queue();
            return;
        }

        if (teamFile.delete()) {
            event.getHook().sendMessage("✅ Team `" + teamName + "` has been removed.").queue();

            // ✅ Log the action to staff audit channel
            StaffLogger.log(jda, StaffLogger.LogType.STAFF,
                    "Team `" + teamName + "` was removed by `" + discordTag + "` (ID: " + discordId + ")"
            );
        } else {
            event.getHook().sendMessage("❌ Failed to delete `" + teamName + "`.").queue();
        }
    }
}