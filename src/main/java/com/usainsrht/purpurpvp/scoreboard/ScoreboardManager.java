package com.usainsrht.purpurpvp.scoreboard;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.match.Match;
import com.usainsrht.purpurpvp.match.MatchState;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * Dynamic scoreboard system.
 * Shows different boards for Lobby, Queue, Duel, FFA, and Spectator contexts.
 */
public class ScoreboardManager {

    private final PurpurPvP plugin;
    private BukkitTask updateTask;

    public ScoreboardManager(PurpurPvP plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                update(player);
            }
        }, 0L, 10L); // every 0.5 seconds
    }

    public void update(Player player) {
        Match match = plugin.getMatchManager().getMatchOfPlayer(player.getUniqueId());

        if (match != null) {
            if (match.isSpectator(player.getUniqueId())) {
                setSpectatorBoard(player, match);
            } else if (match.getConfig().getTeamCount() > 2 || match.getConfig().getTeamSize() > 2) {
                setFFABoard(player, match);
            } else {
                setDuelBoard(player, match);
            }
        } else if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            setQueueBoard(player);
        } else {
            setLobbyBoard(player);
        }
    }

    // ===== Lobby =====

    private void setLobbyBoard(Player player) {
        PlayerProfile profile = plugin.getRankManager().getProfile(player.getUniqueId());
        int online = Bukkit.getOnlinePlayers().size();
        int matches = plugin.getMatchManager().getActiveMatches().size();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("Online: ", NamedTextColor.GRAY).append(Component.text(online, NamedTextColor.WHITE)));
        lines.add(Component.text("Matches: ", NamedTextColor.GRAY).append(Component.text(matches, NamedTextColor.WHITE)));
        lines.add(Component.empty());
        if (profile != null) {
            lines.add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(profile.getLevel(), NamedTextColor.GREEN)));
            lines.add(Component.text("Elo: ", NamedTextColor.GRAY).append(Component.text(String.format("%.0f", profile.getEloRating()), NamedTextColor.YELLOW)));
            lines.add(Component.text("K/D: ", NamedTextColor.GRAY).append(Component.text(
                    profile.getKills() + "/" + profile.getDeaths(), NamedTextColor.AQUA)));
            lines.add(Component.text("W/L: ", NamedTextColor.GRAY).append(Component.text(
                    profile.getWins() + "/" + profile.getLosses(), NamedTextColor.GREEN)));
        }
        lines.add(Component.empty());
        lines.add(Component.text("purpurpvp.com", NamedTextColor.DARK_PURPLE));

        applyBoard(player, Component.text(" PURPURPVP ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), lines);
    }

    // ===== Queue =====

    private void setQueueBoard(Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("Status: ", NamedTextColor.GRAY).append(Component.text("Searching...", NamedTextColor.YELLOW)));
        lines.add(Component.empty());
        lines.add(Component.text("purpurpvp.com", NamedTextColor.DARK_PURPLE));

        applyBoard(player, Component.text(" QUEUE ", NamedTextColor.GOLD, TextDecoration.BOLD), lines);
    }

    // ===== Duel =====

    private void setDuelBoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        // Round info
        int roundsToWin = match.rules().getRoundsToWin();
        lines.add(Component.text("Round: ", NamedTextColor.GRAY).append(
                Component.text(match.getCurrentRound() + "/" + match.getConfig().getBestOf(), NamedTextColor.WHITE)));

        // Score
        for (Team team : match.getTeams()) {
            NamedTextColor color = team.getMembers().contains(player.getUniqueId()) ? NamedTextColor.GREEN : NamedTextColor.RED;
            lines.add(Component.text(team.getName() + ": ", color)
                    .append(Component.text(team.getRoundWins() + "/" + roundsToWin, NamedTextColor.WHITE)));
        }

        lines.add(Component.empty());

        // Kills
        int myKills = match.getKillCounts().getOrDefault(player.getUniqueId(), 0);
        lines.add(Component.text("Your Kills: ", NamedTextColor.GRAY).append(Component.text(myKills, NamedTextColor.WHITE)));

        // Kit
        if (match.getConfig().getKit() != null) {
            lines.add(Component.text("Kit: ", NamedTextColor.GRAY).append(
                    Component.text(match.getConfig().getKit().getName(), NamedTextColor.GOLD)));
        }

        // State
        NamedTextColor stateColor = match.getState() == MatchState.ACTIVE ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        lines.add(Component.text("State: ", NamedTextColor.GRAY).append(
                Component.text(match.getState().name(), stateColor)));

        lines.add(Component.empty());
        lines.add(Component.text("purpurpvp.com", NamedTextColor.DARK_PURPLE));

        applyBoard(player, Component.text(" DUEL ", NamedTextColor.RED, TextDecoration.BOLD), lines);
    }

    // ===== FFA =====

    private void setFFABoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        // Alive count
        long alive = match.getTeams().stream().filter(Team::hasAlivePlayers).count();
        lines.add(Component.text("Alive: ", NamedTextColor.GRAY).append(Component.text(alive, NamedTextColor.GREEN)));

        // My kills
        int myKills = match.getKillCounts().getOrDefault(player.getUniqueId(), 0);
        lines.add(Component.text("Your Kills: ", NamedTextColor.GRAY).append(Component.text(myKills, NamedTextColor.WHITE)));

        lines.add(Component.empty());

        // Top 3 killers
        lines.add(Component.text("Top Killers:", NamedTextColor.GOLD));
        match.getKillCounts().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : "???";
                    lines.add(Component.text(" " + name + ": ", NamedTextColor.GRAY)
                            .append(Component.text(entry.getValue(), NamedTextColor.WHITE)));
                });

        lines.add(Component.empty());
        lines.add(Component.text("purpurpvp.com", NamedTextColor.DARK_PURPLE));

        applyBoard(player, Component.text(" FFA ", NamedTextColor.GOLD, TextDecoration.BOLD), lines);
    }

    // ===== Spectator =====

    private void setSpectatorBoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("Spectating", NamedTextColor.GRAY));

        // Round
        lines.add(Component.text("Round: ", NamedTextColor.GRAY).append(
                Component.text(match.getCurrentRound() + "/" + match.getConfig().getBestOf(), NamedTextColor.WHITE)));

        // Teams
        for (Team team : match.getTeams()) {
            boolean alive = team.hasAlivePlayers();
            lines.add(Component.text(team.getName() + ": ", alive ? NamedTextColor.GREEN : NamedTextColor.RED)
                    .append(Component.text(team.getRoundWins() + " wins", NamedTextColor.WHITE)));
        }

        lines.add(Component.empty());
        lines.add(Component.text("purpurpvp.com", NamedTextColor.DARK_PURPLE));

        applyBoard(player, Component.text(" SPECTATE ", NamedTextColor.GRAY, TextDecoration.BOLD), lines);
    }

    // ===== Core Board Builder =====

    private void applyBoard(Player player, Component title, List<Component> lines) {
        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        // Remove old objective
        Objective old = scoreboard.getObjective("pvp");
        if (old != null) old.unregister();

        Objective obj = scoreboard.registerNewObjective("pvp", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            // Use unique invisible strings as entry names
            String entry = generateEntry(i);
            org.bukkit.scoreboard.Team team = scoreboard.getTeam("line_" + i);
            if (team == null) {
                team = scoreboard.registerNewTeam("line_" + i);
            }
            team.prefix(lines.get(i));
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            obj.getScore(entry).setScore(score - i);
        }

        // Clean up extra lines
        for (int i = lines.size(); i < 20; i++) {
            org.bukkit.scoreboard.Team team = scoreboard.getTeam("line_" + i);
            if (team != null) team.unregister();
        }
    }

    /**
     * Generate a unique invisible entry string for each scoreboard line.
     */
    private String generateEntry(int index) {
        // Use color code sequences to create unique invisible strings
        StringBuilder sb = new StringBuilder();
        String hex = Integer.toHexString(index);
        for (char c : hex.toCharArray()) {
            sb.append("§").append(c);
        }
        sb.append("§r");
        return sb.toString();
    }

    public void removeBoard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeBoard(player);
        }
    }
}

