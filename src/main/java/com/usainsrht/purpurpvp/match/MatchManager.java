package com.usainsrht.purpurpvp.match;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
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

    private final Map<UUID, Map<String, MatchInventorySnapshot>> matchSnapshots = new ConcurrentHashMap<>();

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
        // Dynamically provision an arena instance if not already provided
        if (config.getArenaInstance() == null) {
            String templateName = config.getArena() != null ? config.getArena().getName() : "default";
            var instance = plugin.getDynamicArenaManager().acquireInstance(templateName);
            config.setArenaInstance(instance);
        }

        Match match = new Match(plugin, config, teams);
        activeMatches.put(match.getMatchId(), match);

        for (UUID uuid : match.getAllPlayerUUIDs()) {
            playerMatchMap.put(uuid, match.getMatchId());
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setMetadata("current_instance", new org.bukkit.metadata.FixedMetadataValue(plugin, match.getMatchId().toString()));
                // Preserve inventory state before kit application
                plugin.getLobbyInventoryManager().backup(p);
            }
        }

        if (config.getArena() != null) {
            plugin.getArenaManager().markInUse(config.getArena().getName());
        }

        match.startCountdown();
        return match;
    }

    /**
     * Called when a match ends. Handles cleanup, XP, Elo, post-match summary, and restoration.
     */
    public void endMatch(Match match, Team winner) {
        long duration = Math.max(1, (match.getEndTime() - match.getStartTime()) / 1000);

        // 1. Capture snapshots of players before cleanup
        Map<String, MatchInventorySnapshot> snapshots = new HashMap<>();
        for (UUID uuid : match.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                int kills = match.getKillCounts().getOrDefault(uuid, 0);
                snapshots.put(p.getName().toLowerCase(), new MatchInventorySnapshot(p, kills));
            }
        }
        matchSnapshots.put(match.getMatchId(), snapshots);

        // 2. Award XP and update stats
        plugin.getRankManager().processMatchEnd(match, winner);

        // 3. Save match history async
        plugin.getMatchRepository().saveMatch(
                match.getMatchId(),
                match.getConfig().getArenaInstance() != null ? match.getConfig().getArenaInstance().getTemplate().getName() : "unknown",
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

        // 4. Post-match summary with ItemAPI item text and clickable inspect/rematch actions
        Kit kit = match.getConfig().getKit();
        org.bukkit.inventory.ItemStack kitIcon = kit != null && kit.getIcon() != null
                ? kit.getIcon() : new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD);
        net.kyori.adventure.text.Component kitComp = me.usainsrht.itemapi.itemtext.ItemText.format(kitIcon);

        UUID winnerUuid = winner != null && !winner.getMembers().isEmpty() ? winner.getMembers().iterator().next() : null;
        Player winnerPlayer = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;
        String winnerName = winnerPlayer != null ? winnerPlayer.getName() : (winner != null ? winner.getName() : "None");
        String winnerHealth = winnerPlayer != null ? String.format("%.1f", winnerPlayer.getHealth()) : "0";

        match.broadcastMessage("match.summary-header");
        match.broadcastMessage("match.summary-winner",
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", winnerName),
                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("health", winnerHealth));

        for (Team t : match.getTeams()) {
            if (winner == null || t.getIndex() != winner.getIndex()) {
                for (UUID lUuid : t.getMembers()) {
                    Player lp = Bukkit.getPlayer(lUuid);
                    if (lp != null) {
                        match.broadcastMessage("match.summary-loser", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", lp.getName()));
                    }
                }
            }
        }

        match.broadcastMessage("match.summary-kit", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component("kit", kitComp));
        match.broadcastMessage("match.summary-duration", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("duration", duration + "s"));

        for (UUID uuid : match.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                String opponentName = "";
                for (UUID other : match.getAllPlayerUUIDs()) {
                    if (!other.equals(uuid)) {
                        Player opp = Bukkit.getPlayer(other);
                        if (opp != null) opponentName = opp.getName();
                    }
                }

                for (String pName : snapshots.keySet()) {
                    plugin.getMessageService().sendWithoutPrefix(p, "match.summary-inspect",
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("match_id", match.getMatchId().toString()),
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", pName));
                }

                if (!opponentName.isEmpty()) {
                    plugin.getMessageService().sendWithoutPrefix(p, "match.summary-rematch",
                            net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("opponent", opponentName));
                }
            }
        }

        match.broadcastMessage("match.summary-footer");

        // 5. Schedule delayed teleport and restoration
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Location lobby = plugin.getLobbyLocation();
            for (UUID uuid : match.getAllPlayerUUIDs()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setWalkSpeed(0.2f);
                    p.setFlySpeed(0.1f);
                    p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                    if (lobby != null) p.teleport(lobby);
                    p.removeMetadata("current_instance", plugin);
                    // Restore original inventory & stats
                    plugin.getLobbyInventoryManager().restore(p);
                }
                playerMatchMap.remove(uuid);
            }

            for (UUID uuid : match.getSpectators()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setGameMode(GameMode.SURVIVAL);
                    if (lobby != null) p.teleport(lobby);
                    p.removeMetadata("current_instance", plugin);
                }
            }

            // Release dynamic arena instance or legacy arena
            if (match.getConfig().getArenaInstance() != null) {
                plugin.getDynamicArenaManager().releaseInstance(match.getConfig().getArenaInstance());
            }
            if (match.getConfig().getArena() != null) {
                plugin.getArenaManager().markAvailable(match.getConfig().getArena().getName());
            }

            activeMatches.remove(match.getMatchId());
        }, 70L);
    }

    public MatchInventorySnapshot getSnapshot(UUID matchId, String playerName) {
        Map<String, MatchInventorySnapshot> map = matchSnapshots.get(matchId);
        return map != null ? map.get(playerName.toLowerCase()) : null;
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

