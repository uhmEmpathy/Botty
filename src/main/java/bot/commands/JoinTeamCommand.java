package bot.commands;

import data.DataManager;
import data.models.Player;
import data.models.Team;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class JoinTeamCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("join_team")) return;

        String teamName = event.getOption("team").getAsString().trim().toLowerCase();
        String discordId = event.getUser().getId();
        event.deferReply().queue();

        if (!DataManager.players.containsKey(discordId)) {
            event.getHook().sendMessage("❌ You must register first using `/register` before joining a team.").queue();
            return;
        }

        Team team = DataManager.teams.get(teamName);
        if (team == null) {
            event.getHook().sendMessage("❌ Team not found.").queue();
            return;
        }

        if (team.memberDiscordIds.contains(discordId)) {
            event.getHook().sendMessage("ℹ️ You are already in this team.").queue();
            return;
        }

        team.memberDiscordIds.add(discordId);
        DataManager.save();

        Player player = DataManager.players.get(discordId);
        event.getHook().sendMessage("✅ Added **" + player.ign + "** to team **" + team.name + "**.").queue();
    }
}

