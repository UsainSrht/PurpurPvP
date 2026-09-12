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
        lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.online",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("online", String.valueOf(online))));
        lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.matches",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("matches", String.valueOf(matches))));
        lines.add(Component.empty());
        if (profile != null) {
            lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.level",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("level", String.valueOf(profile.getLevel()))));
            lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.elo",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("elo", String.format("%.0f", profile.getEloRating()))));
            lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.kd",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kills", String.valueOf(profile.getKills())),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("deaths", String.valueOf(profile.getDeaths()))));
            lines.add(plugin.getMessageService().toComponent("scoreboard.lobby.wl",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("wins", String.valueOf(profile.getWins())),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("losses", String.valueOf(profile.getLosses()))));
        }
        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.footer"));

        applyBoard(player, plugin.getMessageService().toComponent("scoreboard.title.lobby"), lines);
    }

    // ===== Queue =====

    private void setQueueBoard(Player player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.queue.searching"));
        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.footer"));

        applyBoard(player, plugin.getMessageService().toComponent("scoreboard.title.queue"), lines);
    }

    // ===== Duel =====

    private void setDuelBoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        // Round info
        int roundsToWin = match.rules().getRoundsToWin();
        lines.add(plugin.getMessageService().toComponent("scoreboard.duel.round",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("current", String.valueOf(match.getCurrentRound())),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(match.getConfig().getBestOf()))));

        // Score
        for (Team team : match.getTeams()) {
            String color = team.getMembers().contains(player.getUniqueId()) ? "<green>" : "<red>";
            lines.add(plugin.getMessageService().toComponent("scoreboard.duel.team-score",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("color", color),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("team", team.getName()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("wins", String.valueOf(team.getRoundWins())),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(roundsToWin))));
        }

        lines.add(Component.empty());

        // Kills
        int myKills = match.getKillCounts().getOrDefault(player.getUniqueId(), 0);
        lines.add(plugin.getMessageService().toComponent("scoreboard.duel.kills",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kills", String.valueOf(myKills))));

        // Kit
        if (match.getConfig().getKit() != null) {
            lines.add(plugin.getMessageService().toComponent("scoreboard.duel.kit",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kit", match.getConfig().getKit().getName())));
        }

        // State
        String stateColor = match.getState() == MatchState.ACTIVE ? "<green>" : "<yellow>";
        lines.add(plugin.getMessageService().toComponent("scoreboard.duel.state",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("color", stateColor),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("state", match.getState().name())));

        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.footer"));

        applyBoard(player, plugin.getMessageService().toComponent("scoreboard.title.duel"), lines);
    }

    // ===== FFA =====

    private void setFFABoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());

        // Alive count
        long alive = match.getTeams().stream().filter(Team::hasAlivePlayers).count();
        lines.add(plugin.getMessageService().toComponent("scoreboard.ffa.alive",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("alive", String.valueOf(alive))));

        // My kills
        int myKills = match.getKillCounts().getOrDefault(player.getUniqueId(), 0);
        lines.add(plugin.getMessageService().toComponent("scoreboard.ffa.kills",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kills", String.valueOf(myKills))));

        lines.add(Component.empty());

        // Top 3 killers
        lines.add(plugin.getMessageService().toComponent("scoreboard.ffa.top-header"));
        match.getKillCounts().entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(3)
                .forEach(entry -> {
                    Player p = Bukkit.getPlayer(entry.getKey());
                    String name = p != null ? p.getName() : "???";
                    lines.add(plugin.getMessageService().toComponent("scoreboard.ffa.top-entry",
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", name),
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("kills", String.valueOf(entry.getValue()))));
                });

        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.footer"));

        applyBoard(player, plugin.getMessageService().toComponent("scoreboard.title.ffa"), lines);
    }

    // ===== Spectator =====

    private void setSpectatorBoard(Player player, Match match) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.spectator.spectating"));

        // Round
        lines.add(plugin.getMessageService().toComponent("scoreboard.spectator.round",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("current", String.valueOf(match.getCurrentRound())),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("max", String.valueOf(match.getConfig().getBestOf()))));

        // Teams
        for (Team team : match.getTeams()) {
            boolean alive = team.hasAlivePlayers();
            lines.add(plugin.getMessageService().toComponent("scoreboard.spectator.team-status",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("color", alive ? "<green>" : "<red>"),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("team", team.getName()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("wins", String.valueOf(team.getRoundWins()))));
        }

        lines.add(Component.empty());
        lines.add(plugin.getMessageService().toComponent("scoreboard.footer"));

        applyBoard(player, plugin.getMessageService().toComponent("scoreboard.title.spectator"), lines);
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

