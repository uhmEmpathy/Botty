
package bot.commands;


import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import util.RoleValidator;
import util.RoleValidator.Role;
import util.StaffLogger;


public class StaffAddCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("staffadd")) return;

        String invokerId = event.getUser().getId();
        if (!RoleValidator.isAdmin(invokerId)) {
            event.reply("❌ You do not have permission to use this command.").setEphemeral(true).queue();
            return;
        }

        OptionMapping userOption = event.getOption("user");
        OptionMapping roleOption = event.getOption("role");

        if (userOption == null || roleOption == null) {
            event.reply("❌ Missing user or role argument. Usage: /staffadd @user STAFF or ADMIN").setEphemeral(true).queue();
            return;
        }

        User user = userOption.getAsUser();
        String roleStr = roleOption.getAsString().toUpperCase();

        Role roleToAssign;
        try {
            roleToAssign = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            event.reply("❌ Invalid role. Must be `STAFF` or `ADMIN`.").setEphemeral(true).queue();
            return;
        }

        // Assign role using patched RoleValidator
        RoleValidator.setRole(user.getId(), roleToAssign);

        // Log the action
        StaffLogger.log(event.getJDA(), StaffLogger.LogType.ADMIN, "✅ Added `" + roleStr + "` access to <@" + user.getId() + "> (" + user.getName() + "#" + user.getDiscriminator() + ")"
        );

        event.reply("✅ Added `" + roleStr + "` access to " + user.getAsMention()).setEphemeral(true).queue();
    }
}
