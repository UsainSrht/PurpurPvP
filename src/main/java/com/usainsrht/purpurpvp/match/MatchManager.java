package com.usainsrht.purpurpvp.match;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.match.team.Team;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all active matches. Creates, destroys, and ticks matches.
 */
public class MatchManager {

    private final PurpurPvP plugin;
    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();  // matchId -> Match
    private final Map<UUID, UUID> playerMatchMap = new ConcurrentHashMap<>();  // playerUUID -> matchId
    private BukkitTask tickTask;

    public MatchManager(PurpurPvP plugin) {
        this.plugin = plugin;
        startTickTask();
    }

    private void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Match match : activeMatches.values()) {
                if (match.getState() == MatchState.COUNTDOWN) {
                    match.tickCountdown();
                }
            }
        }, 0L, 1L);
    }

    /**
     * Creates and starts a new match.
     */
    public Match createMatch(MatchConfig config, List<Team> teams) {
        Match match = new Match(plugin, config, teams);

        activeMatches.put(match.getMatchId(), match);
        for (UUID uuid : match.getAllPlayerUUIDs()) {
            playerMatchMap.put(uuid, match.getMatchId());
            // Set metadata for chat scoping
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setMetadata("current_instance", new org.bukkit.metadata.FixedMetadataValue(plugin, match.getMatchId().toString()));
            }
        }

        // Mark arena in use
        if (config.getArena() != null) {
            plugin.getArenaManager().markInUse(config.getArena().getName());
        }

        match.startCountdown();
        return match;
    }

    /**
     * Called when a match ends. Handles cleanup, XP, Elo, etc.
     */
    public void endMatch(Match match, Team winner) {
        // Calculate duration
        long duration = (match.getEndTime() - match.getStartTime()) / 1000;

        // Award XP and update stats
        plugin.getRankManager().processMatchEnd(match, winner);

        // Save match history
        plugin.getMatchRepository().saveMatch(
                match.getMatchId(),
                match.getConfig().getArena() != null ? match.getConfig().getArena().getName() : "unknown",
                match.getConfig().getKit() != null ? match.getConfig().getKit().getName() : "none",
                match.getConfig().getTeamSize(),
                match.getConfig().getTeamCount(),
                winner != null ? winner.getIndex() : -1,
                match.getCurrentRound(),
                (int) duration,
                match.getConfig().isRanked(),
                match.getStartTime(),
                match.getEndTime()
        );

        // Teleport players back and clean up
        Location lobby = plugin.getLobbyLocation();
        for (UUID uuid : match.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setWalkSpeed(0.2f);
                p.setFlySpeed(0.1f);
                p.getInventory().clear();
                p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                p.setFoodLevel(20);
                p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                if (lobby != null) p.teleport(lobby);
                p.removeMetadata("current_instance", plugin);
            }
            playerMatchMap.remove(uuid);
        }

        // Spectators too
        for (UUID uuid : match.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                if (lobby != null) p.teleport(lobby);
                p.removeMetadata("current_instance", plugin);
            }
        }

        // Mark arena available
        if (match.getConfig().getArena() != null) {
            plugin.getArenaManager().markAvailable(match.getConfig().getArena().getName());
        }

        activeMatches.remove(match.getMatchId());
    }

    // ===== Query Methods =====

    public Match getMatch(UUID matchId) {
        return activeMatches.get(matchId);
    }

    public Match getMatchOfPlayer(UUID playerUuid) {
        UUID matchId = playerMatchMap.get(playerUuid);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    public boolean isInMatch(UUID playerUuid) {
        return playerMatchMap.containsKey(playerUuid);
    }

    public Collection<Match> getActiveMatches() {
        return Collections.unmodifiableCollection(activeMatches.values());
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();

        // Force end all matches
        for (Match match : new ArrayList<>(activeMatches.values())) {
            endMatch(match, null);
        }
    }
}

