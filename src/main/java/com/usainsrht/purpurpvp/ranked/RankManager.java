package com.usainsrht.purpurpvp.ranked;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.database.repository.PlayerRepository;
import com.usainsrht.purpurpvp.match.Match;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.ranked.rating.EloCalculator;
import com.usainsrht.purpurpvp.ranked.rating.Glicko2Calculator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages XP, leveling, and rating calculations.
 */
public class RankManager {

    private final PurpurPvP plugin;
    private final PlayerRepository playerRepo;
    private final EloCalculator eloCalculator;
    private final Glicko2Calculator glicko2Calculator;

    // Cached profiles
    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    private BukkitTask saveTask;

    public RankManager(PurpurPvP plugin, PlayerRepository playerRepo) {
        this.plugin = plugin;
        this.playerRepo = playerRepo;
        this.eloCalculator = new EloCalculator(plugin.getConfigManager().getEloKFactor());
        this.glicko2Calculator = new Glicko2Calculator();

        // Periodic save every 5 minutes
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllDirty, 6000L, 6000L);
    }

    /**
     * Load or create a player profile when they join.
     */
    public void loadProfile(UUID uuid, String username) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Optional<PlayerProfile> opt = playerRepo.findByUuid(uuid);
            PlayerProfile profile;
            if (opt.isPresent()) {
                profile = opt.get();
                profile.setUsername(username);
            } else {
                profile = new PlayerProfile(uuid);
                profile.setUsername(username);
                playerRepo.saveOrUpdate(profile);
            }
            profile.setLastSeen(System.currentTimeMillis());
            profiles.put(uuid, profile);
        });
    }

    /**
     * Save and unload profile on disconnect.
     */
    public void unloadProfile(UUID uuid) {
        PlayerProfile profile = profiles.remove(uuid);
        if (profile != null && profile.isDirty()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerRepo.saveOrUpdate(profile));
        }
    }

    public PlayerProfile getProfile(UUID uuid) {
        return profiles.get(uuid);
    }

    // ===== Match End Processing =====

    /**
     * Process XP, rating, and stats at the end of a match.
     */
    public void processMatchEnd(Match match, Team winnerTeam) {
        long xpPerKill = plugin.getConfigManager().getXpPerKill();
        long xpPerRoundWin = plugin.getConfigManager().getXpPerRoundWin();
        long xpPerMatchWin = plugin.getConfigManager().getXpPerMatchWin();
        long xpPerMatchComplete = plugin.getConfigManager().getXpPerMatchComplete();
        long xpPerLevel = plugin.getConfigManager().getXpPerLevel();

        // Process each team
        for (Team team : match.getTeams()) {
            boolean isWinner = winnerTeam != null && team.getIndex() == winnerTeam.getIndex();

            for (UUID uuid : team.getMembers()) {
                PlayerProfile profile = getProfile(uuid);
                if (profile == null) continue;

                int playerKills = match.getKillCounts().getOrDefault(uuid, 0);

                // XP calculation
                long xpEarned = xpPerMatchComplete;
                xpEarned += playerKills * xpPerKill;
                xpEarned += team.getRoundWins() * xpPerRoundWin;
                if (isWinner) {
                    xpEarned += xpPerMatchWin;
                    profile.incrementWins();
                } else {
                    profile.incrementLosses();
                }

                profile.addKills(playerKills);

                // Add XP and check level up
                long oldXp = profile.getXp();
                int oldLevel = profile.getLevel();
                profile.addXp(xpEarned);

                // Level calculation
                int newLevel = (int) (profile.getXp() / xpPerLevel) + 1;
                if (newLevel > oldLevel) {
                    profile.setLevel(newLevel);
                    onLevelUp(uuid, oldLevel, newLevel);
                }

                // Notify player
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendMessage(Component.text("+" + xpEarned + " XP", NamedTextColor.GREEN));
                }

                // Save participant data
                double eloChange = 0;
                if (match.getConfig().isRanked() && winnerTeam != null) {
                    eloChange = processRating(profile, match, isWinner);
                }

                // Save match participation async
                final double finalEloChange = eloChange;
                final long finalXpEarned = xpEarned;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                        plugin.getMatchRepository().saveParticipant(
                                match.getMatchId(), uuid, team.getIndex(),
                                playerKills, 0, finalEloChange, finalXpEarned));
            }
        }
    }

    /**
     * Process Elo/Glicko-2 rating changes for a player.
     */
    private double processRating(PlayerProfile profile, Match match, boolean won) {
        // Calculate average opponent rating
        double opponentRating = 0;
        int opponentCount = 0;
        for (Team team : match.getTeams()) {
            if (!team.getMembers().contains(profile.getUuid())) {
                for (UUID opp : team.getMembers()) {
                    PlayerProfile oppProfile = getProfile(opp);
                    if (oppProfile != null) {
                        opponentRating += oppProfile.getEloRating();
                        opponentCount++;
                    }
                }
            }
        }
        if (opponentCount == 0) return 0;
        opponentRating /= opponentCount;

        // Elo calculation
        double oldElo = profile.getEloRating();
        double newElo = eloCalculator.calculateNewRating(oldElo, opponentRating, won ? 1.0 : 0.0);
        double eloChange = newElo - oldElo;
        profile.setEloRating(newElo);

        // Glicko-2 calculation
        glicko2Calculator.updateRating(profile, opponentRating, 350.0, won ? 1.0 : 0.0);

        // Notify player
        Player p = Bukkit.getPlayer(profile.getUuid());
        if (p != null) {
            String sign = eloChange >= 0 ? "+" : "";
            p.sendMessage(Component.text(
                    "Elo: " + String.format("%.0f", oldElo) + " → " + String.format("%.0f", newElo) +
                            " (" + sign + String.format("%.0f", eloChange) + ")",
                    eloChange >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
        }

        return eloChange;
    }

    private void onLevelUp(UUID uuid, int oldLevel, int newLevel) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.showTitle(Title.title(
                    Component.text("LEVEL UP!", NamedTextColor.GOLD),
                    Component.text("Level " + oldLevel + " → " + newLevel, NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            p.sendMessage(Component.text("You are now level " + newLevel + "!", NamedTextColor.GOLD));
        }
    }

    // ===== Leaderboard =====

    public List<PlayerProfile> getTopByElo(int limit) {
        return playerRepo.getTopByElo(limit);
    }

    public List<PlayerProfile> getTopByLevel(int limit) {
        return playerRepo.getTopByLevel(limit);
    }

    // ===== Persistence =====

    private void saveAllDirty() {
        for (PlayerProfile profile : profiles.values()) {
            if (profile.isDirty()) {
                playerRepo.saveOrUpdate(profile);
                profile.setDirty(false);
            }
        }
    }

    public void saveAll() {
        for (PlayerProfile profile : profiles.values()) {
            playerRepo.saveOrUpdate(profile);
            profile.setDirty(false);
        }
    }

    public void shutdown() {
        if (saveTask != null) saveTask.cancel();
        saveAll();
    }
}

