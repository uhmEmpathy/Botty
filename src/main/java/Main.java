
import bot.commands.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import util.RoleValidator;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;

public class Main {
    public static void main(String[] args) throws LoginException, InterruptedException {
        String token = "MTM3NjQ2Nzg0NjQyMjU5MzYwNw.GyCvQZ.hDWeOXk6hPYik0ZA_iym7Phx175LPABraMsaQs";
        String guildId = "1028130852506456106";

        RoleValidator.isStaff("0");
        //AdminValidator.isAdmin("0");

        // Ensure required directories and files exist
        File autoTeamsDir = new File("data/auto_teams");
        if (!autoTeamsDir.exists()) {
            autoTeamsDir.mkdirs();
            System.out.println("✅ Created folder: data/auto_teams");
        }

        File teamNamesFile = new File("data/auto_teams/team_names.json");
        if (!teamNamesFile.exists()) {
            JsonArray template = new JsonArray();
            template.add("Player1");
            template.add("Player2");
            template.add("Player3");
            template.add("Player4");
            template.add("Player5");

            try (FileWriter writer = new FileWriter(teamNamesFile)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(template, writer);
                System.out.println("✅ Created file: team_names.json with default players");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        File generatedTeams = new File("data/auto_teams/generated_teams");
        if (!generatedTeams.exists()) {
            generatedTeams.mkdirs();
            System.out.println("✅ Created folder: data/auto_teams/generated_teams");
        }

        JDABuilder builder = JDABuilder.createDefault(token);
        JDA jda = builder.build();

        jda.addEventListener(
                new RegisterCommand(),
                new CheckRankCommand(),
                new CreateTeamCommand(),
                new InviteCommand(),
                new AcceptInviteCommand(),
                new TeamInfoCommand(),
                new TeamsCommand(),
                new LeaveTeamCommand(),
                new KickCommand(),
                new DisbandTeamCommand(),
                new BracketCommand(),
                new AutoCreateTeamsCommand(),
                new StartTournamentCommand(),
                new AdvanceTeamCommand(),
                new RemoveTeamCommand(jda),
                new StaffRemoveCommand(jda),
                new ResetTournamentCommand(),
                new InhouseQueueCommand(),
                new InhouseQueueListCommand(),
                new InhouseStartCommand(),
                new InhousePickCommand(),
                new InhouseTeamsCommand(),
                new InhouseWinnerCommand(),
                new InhouseLadderCommand(),
                new ProfileCommand(),
                new LeagueLeaderboardCommand(),
                new VerifyCommand(),
                new StaffAddCommand(),
                new ReloadCommand(),
                new TrackerCommand()
                );

        jda.awaitReady();
        util.LpTrackerScheduler.start();
        Guild guild = jda.getGuildById(guildId);
        if (guild != null) {
            guild.updateCommands()
                    .addCommands(
                            Commands.slash("reload", "Reloads slash commands (admin only)"),
                            Commands.slash("tracker", "Track LP, rank, and games today for gorillajones#FIGHT."),
                            Commands.slash("staffadd", "Add a Discord user to staff or admin privileges.")
                                    .addOption(OptionType.USER, "user", "The user to grant access to", true)
                                    .addOption(OptionType.STRING, "role", "Access type: STAFF or ADMIN", true),
                            Commands.slash("verify", "Complete your League account verification."),
                            Commands.slash("league-leaderboard", "Display and refresh all registered users' League ranks."),
                            Commands.slash("profile", "View your profile or someone else's.")
                                    .addOption(OptionType.USER, "user", "The user to view (optional)", false),
                            Commands.slash("inhouse-queue", "Join the inhouse queue (max 10 players)."),
                            Commands.slash("inhouse-queuelist", "Show all players currently in the inhouse queue."),
                            Commands.slash("inhouse-start", "Starts the inhouse match and assigns captains."),
                            Commands.slash("pick", "Captains pick players for their teams.")
                                    .addOption(OptionType.STRING, "discordname", "Discord name of the player to pick", true),
                            Commands.slash("inhouse-teams", "Show the current inhouse team rosters."),
                            Commands.slash("winner", "Submit the winning team for the inhouse match.")
                                    .addOption(OptionType.STRING, "team", "The winning team (blue or red)", true),
                            Commands.slash("inhouse-ladder", "Display the inhouse win/loss leaderboard."),
                            Commands.slash("reset_tournament", "Deletes all tournament and team files."),
                            Commands.slash("teams", "List all teams or get details of a specific one")
                                    .addOption(OptionType.STRING, "team", "Team name to show details", false),
                            Commands.slash("advance_team", "Advance a team to the next round")
                                    .addOption(OptionType.STRING, "team", "Team name to advance", true),
                            Commands.slash("create_teams_from_file", "Admin-only: Create teams from a JSON file"),
                            Commands.slash("bracket", "Show all current tournament matchups"),
                            Commands.slash("start_tournament", "Begin tournament, rank all full teams, and generate matchups"),
                            Commands.slash("disband_team", "Disband and delete your entire team (creator only)"),
                            Commands.slash("kick", "Remove a member from your team")
                                    .addOption(OptionType.USER, "user", "The user to kick", true),
                            Commands.slash("leave_team", "Leave the team you are currently in"),
                            Commands.slash("staffremove", "Remove a Discord user from staff privileges")
                                    .addOption(OptionType.USER, "user", "The user to remove from staff", true),
                            Commands.slash("remove_team", "Delete a team by name")
                                    .addOption(OptionType.STRING, "name", "Team name to delete", true),
                            Commands.slash("teaminfo", "Shows the team you're currently in"),
                            Commands.slash("accept", "Accept an invite to a team")
                                    .addOption(OptionType.STRING, "team", "Team name you were invited to", true),
                            Commands.slash("invite", "Invite a registered player to your team")
                                    .addOption(OptionType.USER, "user", "The user to invite", true),
                            Commands.slash("create_team", "Create a new team")
                                    .addOption(OptionType.STRING, "name", "Your team name", true),
                            Commands.slash("register", "Register your in-game name")
                                    .addOption(OptionType.STRING, "ign", "Your Riot ID (e.g., Empathy#NA3)", true),
                            Commands.slash("checkrank", "Check your current rank")
                                    .addOption(OptionType.STRING, "ign", "Your Riot ID (e.g., Empathy#NA3)", true)
                    ).queue();

            System.out.println("✅ Slash commands registered to guild: " + guild.getName());
        } else {
            System.err.println("❌ Guild not found! Check if your bot is in the server and the ID is correct.");
        }
    }


}