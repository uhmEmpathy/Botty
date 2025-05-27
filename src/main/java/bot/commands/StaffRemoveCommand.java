package bot.commands;

import util.RoleValidator;
import util.StaffLogger;
//import util.enums.LogType;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

public class StaffRemoveCommand extends ListenerAdapter {
    private final JDA jda;

    public StaffRemoveCommand(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equals("staffremove")) return;

        event.deferReply().queue();

        String senderId = event.getUser().getId();
        String senderTag = event.getUser().getAsTag();
        String targetId = event.getOption("user").getAsUser().getId();

        if (!RoleValidator.isAdmin(senderId)) {
            event.getHook().sendMessage("❌ Only Admins can use this command.").queue();
            return;
        }

        File file = new File("data/privileges/roles.json");

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> roleMap = new HashMap<>();

            if (file.exists()) {
                roleMap = mapper.readValue(file, Map.class);
            } else {
                event.getHook().sendMessage("❌ Role file not found.").queue();
                return;
            }

            if (!roleMap.containsKey(targetId)) {
                event.getHook().sendMessage("⚠️ <@" + targetId + "> is not currently a staff member.").queue();
                return;
            }

            roleMap.remove(targetId);

            try (FileWriter writer = new FileWriter(file)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, roleMap);
            }

            event.getHook().sendMessage("✅ Removed <@" + targetId + "> from staff.").queue();

            StaffLogger.log(jda, StaffLogger.LogType.ADMIN,
                    "Admin `" + senderTag + "` (ID: " + senderId + ") removed <@" + targetId + "> from staff."
            );

        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().sendMessage("❌ Failed to remove staff.").queue();
        }
    }
}