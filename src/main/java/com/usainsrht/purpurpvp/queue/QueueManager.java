package com.usainsrht.purpurpvp.queue;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.match.MatchConfig;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.match.team.TeamAllocator;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Manages matchmaking queues for ranked and unranked modes.
 */
public class QueueManager {

    private final PurpurPvP plugin;

    // Queues per mode: "1v1_ranked", "1v1_unranked", "2v2_ranked", etc.
    private final Map<String, Deque<QueueEntry>> queues = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerQueueMap = new ConcurrentHashMap<>(); // playerUUID -> mode

    private BukkitTask matchmakingTask;

    // Elo matching config
    private static final double BASE_RANGE = 100.0;
    private static final double EXPANSION_PER_SECOND = 5.0;
    private static final double MAX_RANGE = 500.0;

    public QueueManager(PurpurPvP plugin) {
        this.plugin = plugin;
        startMatchmakingTask();
    }

    private void startMatchmakingTask() {
        matchmakingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processQueues, 40L, 40L); // every 2 seconds
    }

    /**
     * Add a player to a queue.
     */
    public boolean joinQueue(UUID playerUuid, String mode) {
        if (playerQueueMap.containsKey(playerUuid)) return false;
        if (plugin.getMatchManager().isInMatch(playerUuid)) return false;
        if (plugin.getRoomManager().isInRoom(playerUuid)) return false;

        PlayerProfile profile = plugin.getRankManager().getProfile(playerUuid);
        double rating = profile != null ? profile.getEloRating() : 1000.0;

        QueueEntry entry = new QueueEntry(playerUuid, rating);
        queues.computeIfAbsent(mode, k -> new ConcurrentLinkedDeque<>()).add(entry);
        playerQueueMap.put(playerUuid, mode);

        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null) {
            p.sendMessage(Component.text("You joined the " + mode + " queue. Searching...", NamedTextColor.GREEN));
        }

        return true;
    }

    /**
     * Remove a player from their queue.
     */
    public void leaveQueue(UUID playerUuid) {
        String mode = playerQueueMap.remove(playerUuid);
        if (mode == null) return;

        Deque<QueueEntry> queue = queues.get(mode);
        if (queue != null) {
            queue.removeIf(e -> e.getPlayerUuid().equals(playerUuid));
        }

        Player p = Bukkit.getPlayer(playerUuid);
        if (p != null) {
            p.sendMessage(Component.text("You left the queue.", NamedTextColor.RED));
        }
    }

    public boolean isInQueue(UUID playerUuid) {
        return playerQueueMap.containsKey(playerUuid);
    }

    /**
     * Process all queues and try to find matches.
     */
    private void processQueues() {
        for (Map.Entry<String, Deque<QueueEntry>> entry : queues.entrySet()) {
            String mode = entry.getKey();
            Deque<QueueEntry> queue = entry.getValue();

            // Remove offline players
            queue.removeIf(e -> {
                Player p = Bukkit.getPlayer(e.getPlayerUuid());
                if (p == null || !p.isOnline()) {
                    playerQueueMap.remove(e.getPlayerUuid());
                    return true;
                }
                return false;
            });

            processQueue(mode, queue);
        }
    }

    private void processQueue(String mode, Deque<QueueEntry> queue) {
        if (queue.size() < 2) return;

        boolean ranked = mode.contains("ranked");
        int teamSize = parseTeamSize(mode);
        int teamCount = 2;
        int requiredPlayers = teamSize * teamCount;

        if (queue.size() < requiredPlayers) return;

        List<QueueEntry> entries = new ArrayList<>(queue);

        // Sort by rating for better matching
        entries.sort(Comparator.comparingDouble(QueueEntry::getRating));

        // Try to find close-rated pairs/groups
        for (int i = 0; i <= entries.size() - requiredPlayers; i++) {
            List<QueueEntry> group = entries.subList(i, i + requiredPlayers);

            double minRating = group.stream().mapToDouble(QueueEntry::getRating).min().orElse(0);
            double maxRating = group.stream().mapToDouble(QueueEntry::getRating).max().orElse(0);
            double maxWait = group.stream().mapToLong(QueueEntry::getWaitTime).max().orElse(0) / 1000.0;

            double allowedRange = Math.min(BASE_RANGE + EXPANSION_PER_SECOND * maxWait, MAX_RANGE);

            if (!ranked || (maxRating - minRating) <= allowedRange) {
                // Match found!
                createMatchFromQueue(group, mode, ranked, teamSize, teamCount);

                // Remove from queue
                for (QueueEntry e : group) {
                    queue.remove(e);
                    playerQueueMap.remove(e.getPlayerUuid());
                }
                return;
            }
        }
    }

    private void createMatchFromQueue(List<QueueEntry> entries, String mode,
                                      boolean ranked, int teamSize, int teamCount) {
        MatchConfig config = new MatchConfig();
        config.setTeamSize(teamSize);
        config.setTeamCount(teamCount);
        config.setRanked(ranked);
        config.getGameRules().setRoundsToWin(ranked ? 2 : 1);

        // Use default kit (first global kit) or random global kit
        Collection<Kit> globalKits = plugin.getKitManager().getAllGlobalKits();
        if (!globalKits.isEmpty()) {
            config.setKit(globalKits.iterator().next());
        }

        // Find available arena
        Arena arena = plugin.getArenaManager().getAvailableArena(teamCount);
        if (arena == null) {
            for (QueueEntry e : entries) {
                Player p = Bukkit.getPlayer(e.getPlayerUuid());
                if (p != null) {
                    p.sendMessage(Component.text("No arena available. Staying in queue...", NamedTextColor.YELLOW));
                }
                // Put back in queue
                queues.computeIfAbsent(mode, k -> new ConcurrentLinkedDeque<>()).add(e);
                playerQueueMap.put(e.getPlayerUuid(), mode);
            }
            return;
        }
        config.setArena(arena);

        // Notify players
        for (QueueEntry e : entries) {
            Player p = Bukkit.getPlayer(e.getPlayerUuid());
            if (p != null) {
                p.sendMessage(Component.text("Match found! Preparing...", NamedTextColor.GREEN));
            }
        }

        // Allocate teams
        List<UUID> playerUuids = entries.stream().map(QueueEntry::getPlayerUuid).toList();
        List<Team> teams = TeamAllocator.allocateRandom(playerUuids, teamCount, teamSize);

        // Create match
        plugin.getMatchManager().createMatch(config, teams);
    }

    private int parseTeamSize(String mode) {
        // modes: "1v1_ranked", "2v2_unranked", etc.
        try {
            return Integer.parseInt(mode.substring(0, 1));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void shutdown() {
        if (matchmakingTask != null) matchmakingTask.cancel();
    }

    public Set<String> getAvailableModes() {
        return Set.of("1v1_ranked", "1v1_unranked", "2v2_ranked", "2v2_unranked", "3v3_ranked", "3v3_unranked");
    }
}


