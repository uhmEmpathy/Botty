package bot.commands;

import data.DataManager;
import data.models.Team;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AddTeamCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("add_team")) return;

        String teamName = event.getOption("name").getAsString().trim();
        event.deferReply().queue();

        if (DataManager.teams.containsKey(teamName.toLowerCase())) {
            event.getHook().sendMessage("❌ A team with that name already exists.").queue();
            return;
        }

        Team team = new Team(teamName);
        DataManager.teams.put(teamName.toLowerCase(), team);
        DataManager.save();

        event.getHook().sendMessage("✅ Team **" + teamName + "** created successfully.").queue();
    }
}
