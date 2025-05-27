package bot.commands;

import util.RoleValidator;
import util.StaffLogger;
//import util.enums.LogType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.io.File;

public class ResetTournamentCommand extends ListenerAdapter {
    private static final String TOURNAMENT_FOLDER = "data/tournament/";
    private static final String TEAMS_FOLDER = "data/teams/";

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reset_tournament")) return;

        String userId = event.getUser().getId();
        if (!RoleValidator.isStaffOrAdmin(userId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        event.reply("⚠️ Are you sure you want to reset all tournament and team data?")
                .addActionRow(
                        Button.danger("confirm_reset", "Yes, Reset"),
                        Button.secondary("cancel_reset", "Cancel")
                )
                .setEphemeral(true)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = event.getUser().getId();
        if (!event.getComponentId().equals("confirm_reset") && !event.getComponentId().equals("cancel_reset")) return;

        if (!RoleValidator.isStaffOrAdmin(userId)) {
            event.reply("❌ You do not have permission to use this.").setEphemeral(true).queue();
            return;
        }

        if (event.getComponentId().equals("cancel_reset")) {
            event.reply("❌ Reset cancelled.").setEphemeral(true).queue();
            return;
        }

        boolean deletedTournament = deleteFolderContents(new File(TOURNAMENT_FOLDER));
        boolean deletedTeams = deleteFolderContents(new File(TEAMS_FOLDER));

        if (deletedTournament && deletedTeams) {
            StaffLogger.log(event.getJDA(), StaffLogger.LogType.STAFF, "🗑️ ``reset_tournament`` confirmed by <@" + userId + ">. All tournament/team data was wiped.");
            event.reply("✅ Tournament data has been reset.").setEphemeral(true).queue();
        } else {
            event.reply("⚠️ Failed to fully reset tournament files. Check permissions.").setEphemeral(true).queue();
        }
    }

    private boolean deleteFolderContents(File folder) {
        if (!folder.exists() || !folder.isDirectory()) return false;

        boolean success = true;
        File[] files = folder.listFiles();
        if (files == null) return false;

        for (File file : files) {
            if (!file.delete()) {
                success = false;
            }
        }
        return success;
    }
}