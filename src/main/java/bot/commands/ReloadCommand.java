package bot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import util.RoleValidator;

public class ReloadCommand extends ListenerAdapter {

    private static final String GUILD_ID = "1028130852506456106"; // Your server ID

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reload")) return;

        String userId = event.getUser().getId();
        if (!RoleValidator.isAdmin(userId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getJDA().getGuildById(GUILD_ID);
        if (guild == null) {
            event.reply("❌ Guild not found.").setEphemeral(true).queue();
            return;
        }

        guild.updateCommands().addCommands(
                Commands.slash("register", "Register your League account")
                        .addOption(OptionType.STRING, "riotid", "Your Riot ID (e.g. Empathy #NA)", true),

                Commands.slash("create_team", "Create a team")
                        .addOption(OptionType.STRING, "name", "Team name", true),

                Commands.slash("invite", "Invite a player to your team")
                        .addOption(OptionType.USER, "user", "User to invite", true),

                Commands.slash("leave_team", "Leave your current team"),
                Commands.slash("kick", "Kick a member from your team")
                        .addOption(OptionType.USER, "user", "User to kick", true),

                Commands.slash("disband_team", "Disband your team"),
                Commands.slash("start_tournament", "Start the tournament"),
                Commands.slash("bracket", "View tournament bracket"),
                Commands.slash("lock_teams", "Lock all teams for the tournament"),

                Commands.slash("advance_team", "Advance a team")
                        .addOption(OptionType.STRING, "team_name", "Name of the team to advance", true),

                Commands.slash("remove_team", "Remove a team")
                        .addOption(OptionType.STRING, "team_name", "Name of the team to remove", true),

                Commands.slash("staffadd", "Add a staff member")
                        .addOption(OptionType.STRING, "userid", "Discord ID of the staff member", true),

                Commands.slash("staffremove", "Remove a staff member")
                        .addOption(OptionType.STRING, "userid", "Discord ID of the staff member", true),

                Commands.slash("reload", "Reload all slash commands (admin only)"),

                Commands.slash("league-leaderboard", "View the League leaderboard"),
                Commands.slash("emote_ids", "View rank emote IDs"),
                Commands.slash("teaminfo", "View info about your current team")
        ).queue(
                success -> event.reply("✅ Slash commands reloaded successfully.").setEphemeral(true).queue(),
                error -> event.reply("❌ Failed to reload commands.").setEphemeral(true).queue()
        );
    }
}
