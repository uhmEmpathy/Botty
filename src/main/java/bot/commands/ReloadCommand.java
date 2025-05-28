package bot.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.RoleValidator;

public class ReloadCommand extends ListenerAdapter {

    private final String guildId = "1028130852506456106"; // Your guild/server ID

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("reload")) return;

        String userId = event.getUser().getId();

        if (!RoleValidator.isAdmin(userId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        Guild guild = event.getJDA().getGuildById(guildId);

        if (guild == null) {
            event.reply("❌ Guild not found. Cannot reload commands.").setEphemeral(true).queue();
            return;
        }

        guild.updateCommands().queue(
                success -> event.reply("✅ Slash commands reloaded successfully.").setEphemeral(true).queue(),
                error -> event.reply("❌ Failed to reload slash commands.").setEphemeral(true).queue()
        );
    }
}
