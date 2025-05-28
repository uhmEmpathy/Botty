package bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

public class LeagueLeaderboardCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("league-leaderboard")) return;

        event.deferReply().queue(); // Acknowledge the slash command

        // Simulate leaderboard generation
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard")
                .setDescription("Sorted leaderboard goes here")
                .setColor(Color.BLUE);

        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().equals("refresh_leaderboard")) return;

        event.deferEdit().queue(); // Acknowledge the button interaction

        // Simulate refreshed leaderboard
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("League Leaderboard (Refreshed)")
                .setDescription("Updated leaderboard content")
                .setColor(Color.GREEN);

        Button refreshButton = Button.primary("refresh_leaderboard", "🔄 Refresh");

        event.getHook().editOriginalEmbeds(embed.build())
                .setActionRow(refreshButton)
                .queue();
    }
}
