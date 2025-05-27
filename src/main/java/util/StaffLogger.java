package util;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StaffLogger {

    // Replace these with your actual channel IDs
    public static final String STAFF_AUDIT_CHANNEL_ID = "1376464814741917758";
    public static final String ADMIN_AUDIT_CHANNEL_ID = "1376464814741917758";

    public enum LogType {
        STAFF, ADMIN
    }

    public static void log(JDA jda, LogType type, String actionMessage) {
        String channelId = switch (type) {
            case STAFF -> STAFF_AUDIT_CHANNEL_ID;
            case ADMIN -> ADMIN_AUDIT_CHANNEL_ID;
        };

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel != null) {
            String prefix = type == LogType.ADMIN ? "🔐 **[ADMIN ACTION]** " : "🛠️ **[STAFF ACTION]** ";
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            channel.sendMessage(prefix + actionMessage + " at `" + timestamp + "`.").queue();
            System.out.println("[StaffLogger] Successfully logged to " + type + " channel: " + channel.getName());
        } else {
            System.err.println("[StaffLogger] ❌ Could not find channel for ID: " + channelId + " (" + type + ")");
        }
    }
}
