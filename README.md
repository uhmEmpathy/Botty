# 🎮 Botty

A full-featured Java-based Discord bot built using JDA for managing competitive League of Legends tournaments, player registrations, inhouse games, team brackets, and more — powered by the Riot API.

---

## ⚙️ Features

### 🧾 Account Registration & Verification
- `/register <IGN#TAG>`  
  Register a Riot account to your Discord ID.
- 🔒 Icon-based verification using default League icons.
- `/verify`  
  Confirms account ownership and stores rank/UID.
- ✅ Automatically assigns a Discord rank role based on verified rank.

---

### 👥 Team System
- `/create_team <name>`  
  Create a new team after registration and rank validation.
- `/invite @user`  
  Invite registered users to your team.
- `/accept <team>`  
  Accept an invite to a team.
- `/leave_team`  
  Leave your current team.
- `/kick @user`  
  Kick a member (team creator only).
- `/disband_team`  
  Disband and delete your team (creator only).
- 🔒 Validates rank eligibility via Riot API.

---

### 🧾 Tournament System
- `/start_tournament`  
  Begins a bracket using team LP for matchmaking.
- `/bracket`  
  View current matchups.
- `/advance_team <team>`  
  Manually advance a team.
- `/remove_team <name>`  
  Staff-only command to delete a team.
- `/reset_tournament`  
  Deletes all team and bracket data.

---

### 📊 League Rank Leaderboard
- `/league-leaderboard`  
  Displays and updates player ranks with LP change tracking:
  - 🔼 Highest LP Gained Today
  - 🔽 Highest LP Lost Today
  - 💠 Rank emojis, win/loss, and live refresh button
- ✅ Daily LP refresh system (3:00 PM EST)

---

### 🧠 Profile Tools
- `/profile [@user]`  
  Shows in-game name, rank, and inhouse stats for a user.
- `/checkrank <IGN#TAG>`  
  Check current rank via Riot API.

---

### 🏠 Inhouse Match System
- `/inhouse-queue`  
  Join the 10-player queue.
- `/inhouse-queuelist`  
  View queue status.
- `/inhouse-start`  
  Starts match and assigns captains.
- `/pick <discordname>`  
  Pick player as captain.
- `/inhouse-teams`  
  Show red and blue team rosters.
- `/winner <red/blue>`  
  Submit winning team.
- `/inhouse-ladder`  
  View leaderboard based on inhouse wins/losses.

---

### 🕵️ Staff & Admin Tools
- Custom permissions system using Discord user IDs (not roles)
- `/staffadd @user STAFF/ADMIN`  
  Add staff/admin with logging.
- `/staffremove @user`  
  Remove privileges.
- 🔒 Logs all changes to a staff audit log channel.

---

### 📈 Account Tracker
- `/tracker`  
  Tracks `gorillajones#FIGHT`:
  - Live rank and LP
  - Games played today
  - LP gained/lost today
  - Starting LP snapshot (resets at 2:45 AM EST)

---

## 🧪 Built With
- [JDA](https://github.com/DV8FromTheWorld/JDA) - Java Discord API
- [Riot API](https://developer.riotgames.com/) - League of Legends data
- Java 17+
- Gson for JSON parsing
- Persistent file-based data storage (`data/`)

---

## 📁 File Structure
