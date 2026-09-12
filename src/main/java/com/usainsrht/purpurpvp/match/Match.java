package com.usainsrht.purpurpvp.match;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.util.Cooldowns;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/**
 * Core match lifecycle management.
 * Handles rounds, team tracking, win conditions, player death/disconnect.
 */
public class Match {

    private final UUID matchId;
    private final MatchConfig config;
    private final List<Team> teams;
    private final Set<UUID> spectators = new LinkedHashSet<>();
    private final Map<UUID, Integer> playerTeamMap = new HashMap<>(); // player UUID -> team index
    private final Map<UUID, Integer> killCounts = new HashMap<>();
    private final Cooldowns cooldowns = new Cooldowns();
    private final Set<Location> changedBlocks = new LinkedHashSet<>();
    private final Map<Location, Material> originalBlocks = new LinkedHashMap<>();

    private MatchState state;
    private int currentRound;
    private int countdownTicks;
    private long startTime;
    private long endTime;
    private BukkitTask worldBorderTask;

    private final PurpurPvP plugin;

    public Match(PurpurPvP plugin, MatchConfig config, List<Team> teams) {
        this.plugin = plugin;
        this.matchId = UUID.randomUUID();
        this.config = config;
        this.teams = teams;
        this.state = MatchState.WAITING;
        this.currentRound = 0;

        // Build player -> team mapping
        for (Team team : teams) {
            for (UUID uuid : team.getMembers()) {
                playerTeamMap.put(uuid, team.getIndex());
                killCounts.put(uuid, 0);
            }
        }
    }

    // ===== Lifecycle =====

    public void startCountdown() {
        state = MatchState.COUNTDOWN;
        countdownTicks = plugin.getConfigManager().getCountdownSeconds() * 20;
        teleportToSpawns();
        applyKit();
        freezePlayers(true);
    }

    /**
     * Called every tick during countdown.
     */
    public void tickCountdown() {
        if (state != MatchState.COUNTDOWN) return;
        countdownTicks--;
        int secondsLeft = countdownTicks / 20;

        if (countdownTicks % 20 == 0 && secondsLeft > 0 && secondsLeft <= 5) {
            broadcastMessage("match.countdown", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("seconds", String.valueOf(secondsLeft)));
        }

        if (countdownTicks <= 0) {
            startRound();
        }
    }

    public void startRound() {
        currentRound++;
        state = MatchState.ACTIVE;
        startTime = System.currentTimeMillis();

        // Reset alive status
        for (Team team : teams) {
            team.resetAlive();
        }

        // Map reset between rounds
        if (currentRound > 1 && config.getGameRules().isMapResetBetweenRounds()) {
            resetMap();
        }

        teleportToSpawns();

        // Inventory reset between rounds (re-apply kit) or keep current
        if (currentRound == 1 || config.getGameRules().isInventoryResetBetweenRounds()) {
            applyKit();
        } else {
            // Just heal and restore gamemode
            for (UUID uuid : getAllPlayerUUIDs()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.setGameMode(GameMode.SURVIVAL);
                    p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
                }
            }
        }

        freezePlayers(false);

        broadcastMessage("match.started", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("round", String.valueOf(currentRound)));

        // Apply game rules
        applyGameRules();
    }

    /**
     * Called when a player dies in this match.
     */
    public void onPlayerDeath(UUID deadPlayer, UUID killer) {
        if (state != MatchState.ACTIVE) return;

        Integer teamIndex = playerTeamMap.get(deadPlayer);
        if (teamIndex == null) return;

        teams.get(teamIndex).markDead(deadPlayer);

        // Track kills
        if (killer != null) {
            killCounts.merge(killer, 1, Integer::sum);
        }

        // Make dead player spectator
        Player dead = Bukkit.getPlayer(deadPlayer);
        if (dead != null) {
            dead.setGameMode(GameMode.SPECTATOR);
            spectators.add(deadPlayer);
        }

        broadcastMessage("match.eliminated", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", dead != null ? dead.getName() : "A player"));

        checkRoundEnd();
    }

    /**
     * Called when a player disconnects during the match.
     */
    public void onPlayerDisconnect(UUID uuid) {
        Integer teamIndex = playerTeamMap.get(uuid);
        if (teamIndex == null) return;

        teams.get(teamIndex).markDead(uuid);
        checkRoundEnd();
    }

    private void checkRoundEnd() {
        List<Team> aliveTeams = teams.stream()
                .filter(Team::hasAlivePlayers)
                .toList();

        if (aliveTeams.size() <= 1) {
            endRound(aliveTeams.isEmpty() ? null : aliveTeams.getFirst());
        }
    }

    private void endRound(Team winner) {
        state = MatchState.ROUND_END;

        if (winner != null) {
            winner.incrementRoundWins();
            broadcastMessage("match.round-win",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("round", String.valueOf(currentRound)),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("team", winner.getName()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("score", getRoundScore()));
        } else {
            broadcastMessage("match.draw", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("round", String.valueOf(currentRound)));
        }

        // Check match win
        if (winner != null && winner.getRoundWins() >= config.getWinsNeeded()) {
            endMatch(winner);
            return;
        }

        // Check if all rounds played (for draws)
        if (currentRound >= config.getBestOf()) {
            // Find team with most wins
            Team matchWinner = teams.stream()
                    .max(Comparator.comparingInt(Team::getRoundWins))
                    .orElse(null);
            endMatch(matchWinner);
            return;
        }

        // Start next round after delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (state == MatchState.ROUND_END) {
                startRound();
            }
        }, 60L); // 3 second delay
    }

    private void endMatch(Team winner) {
        state = MatchState.FINISHED;
        endTime = System.currentTimeMillis();

        // Cancel world border task
        if (worldBorderTask != null) {
            worldBorderTask.cancel();
            worldBorderTask = null;
        }

        // Reset map blocks
        if (config.getGameRules().isMapResetBetweenRounds()) {
            resetMap();
        }

        if (winner != null) {
            broadcastMessage("match.match-win",
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("team", winner.getName()),
                    net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("score", getRoundScore()));
        }

        // Notify match manager immediately to handle cleanup, XP, Elo, post-match summary
        plugin.getMatchManager().endMatch(this, winner);
    }

    // ===== Helpers =====

    private void teleportToSpawns() {
        if (config.getArenaInstance() != null) {
            var instance = config.getArenaInstance();
            for (Team team : teams) {
                List<Location> spawns = team.getIndex() < instance.getTeamSpawns().size()
                        ? instance.getTeamSpawns().get(team.getIndex())
                        : Collections.emptyList();

                int spawnIdx = 0;
                for (UUID uuid : team.getMembers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && !spawns.isEmpty()) {
                        p.teleport(spawns.get(spawnIdx % spawns.size()));
                        spawnIdx++;
                    }
                }
            }
            return;
        }

        Arena arena = config.getArena();
        if (arena == null) return;

        for (Team team : teams) {
            List<Location> spawns = team.getIndex() < arena.getTeamSpawns().size()
                    ? arena.getTeamSpawns().get(team.getIndex())
                    : Collections.emptyList();

            int spawnIdx = 0;
            for (UUID uuid : team.getMembers()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && !spawns.isEmpty()) {
                    p.teleport(spawns.get(spawnIdx % spawns.size()));
                    spawnIdx++;
                }
            }
        }
    }

    private void applyKit() {
        Kit kit = config.getKit();
        if (kit == null) return;

        for (UUID uuid : getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setGameMode(GameMode.SURVIVAL);
                p.setHealth(p.getAttribute(Attribute.MAX_HEALTH).getValue());
                p.setFoodLevel(20);
                p.setSaturation(20f);
                p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                plugin.getKitManager().applyKit(p, kit);
            }
        }
    }

    private void freezePlayers(boolean freeze) {
        for (UUID uuid : getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.setWalkSpeed(freeze ? 0f : 0.2f);
                p.setFlySpeed(freeze ? 0f : 0.1f);
            }
        }
    }

    private void applyGameRules() {
        GameRules rules = config.getGameRules();
        Arena arena = config.getArena();

        // Find the match world
        World world = null;
        if (config.getArenaInstance() != null) {
            world = config.getArenaInstance().getOrigin().getWorld();
        } else if (arena != null && arena.getWorld() != null) {
            world = Bukkit.getWorld(arena.getWorld());
        }

        // Daytime
        if (world != null && rules.getDaytime() != null) {
            world.setTime(rules.getDaytime().getTicks());
            setBooleanGameRule(world, org.bukkit.GameRules.ADVANCE_TIME, false);
        }

        // Weather
        if (world != null && rules.getWeather() != null) {
            setBooleanGameRule(world, org.bukkit.GameRules.ADVANCE_WEATHER, false);
            switch (rules.getWeather()) {
                case CLEAR -> { world.setStorm(false); world.setThundering(false); }
                case RAIN -> { world.setStorm(true); world.setThundering(false); }
                case THUNDER -> { world.setStorm(true); world.setThundering(true); }
                case SNOW -> { world.setStorm(true); world.setThundering(false); }
            }
        }

        // Natural regen
        if (world != null) {
            setBooleanGameRule(world, org.bukkit.GameRules.NATURAL_HEALTH_REGENERATION, rules.isNaturalRegen());
        }

        // Hunger / Food
        for (UUID uuid : getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (!rules.isHunger()) {
                    p.setFoodLevel(20);
                    p.setSaturation(20f);
                }
                // Apply permanent potion effects
                applyPermanentEffects(p);
            }
        }

        // World border
        if (rules.isWorldBorderEnabled() && world != null) {
            WorldBorder border = world.getWorldBorder();
            Location center;
            double initialSize;

            if (config.getArenaInstance() != null) {
                center = config.getArenaInstance().getWorldBorderCenter();
                initialSize = config.getArenaInstance().getTemplate().getWorldBorderInitialSize();
            } else if (arena != null) {
                center = arena.getWorldBorderCenter() != null
                        ? arena.getWorldBorderCenter()
                        : (arena.getSpectatorSpawn() != null ? arena.getSpectatorSpawn() : arena.getTeamSpawns().getFirst().getFirst());
                initialSize = arena.getWorldBorderInitialSize();
            } else {
                center = world.getSpawnLocation();
                initialSize = 60.0;
            }

            border.setCenter(center);
            border.setSize(initialSize);
            border.setDamageAmount(rules.getWorldBorderDamage());
            border.setDamageBuffer(0);

            // Schedule border close
            int closeAfterTicks = rules.getWorldBorderCloseAfterSeconds() * 20;
            worldBorderTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                broadcastMessage("match.border-shrinking");
                border.setSize(1);
                // Use the time-based overload through scheduling progressive shrinking
                Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                    double current = border.getSize();
                    if (current <= 1) { task.cancel(); return; }
                    double step = rules.getWorldBorderCloseSpeed() * 0.5; // runs every 10 ticks = 0.5s
                    border.setSize(Math.max(1, current - step));
                }, 0L, 10L);
            }, closeAfterTicks);
        }
    }

    /**
     * Sets a boolean GameRule safely using the typed constant.
     */
    private void setBooleanGameRule(World world, GameRule<Boolean> rule, boolean value) {
        world.setGameRule(rule, value);
    }

    /**
     * Apply permanent potion effects configured in game rules.
     * These survive milk and death.
     */
    public void applyPermanentEffects(Player player) {
        GameRules rules = config.getGameRules();
        for (Map.Entry<PotionEffectType, Integer> entry : rules.getPermanentEffects().entrySet()) {
            player.addPotionEffect(new PotionEffect(
                    entry.getKey(), PotionEffect.INFINITE_DURATION, entry.getValue(), false, false, true));
        }
    }

    /**
     * Track a block change for map reset.
     */
    public void trackBlockChange(Location loc, Material originalType) {
        if (config.getArenaInstance() != null) {
            config.getArenaInstance().trackBlockChange(loc, originalType);
        }
        if (!originalBlocks.containsKey(loc)) {
            originalBlocks.put(loc.clone(), originalType);
        }
        changedBlocks.add(loc.clone());
    }

    /**
     * Reset all changed blocks to their original state.
     */
    private void resetMap() {
        if (config.getArenaInstance() != null) {
            config.getArenaInstance().cleanAndReset();
        }
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }
        originalBlocks.clear();
        changedBlocks.clear();
    }

    /** Shortcut to access game rules. */
    public GameRules rules() {
        return config.getGameRules();
    }

    public Cooldowns getCooldowns() { return cooldowns; }

    public void broadcastAll(Component message) {
        for (UUID uuid : getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    public void broadcastMessage(String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... tags) {
        for (UUID uuid : getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getMessageService().send(p, key, tags);
        }
        for (UUID uuid : spectators) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getMessageService().send(p, key, tags);
        }
    }

    public Set<UUID> getAllPlayerUUIDs() {
        Set<UUID> all = new LinkedHashSet<>();
        for (Team team : teams) {
            all.addAll(team.getMembers());
        }
        return all;
    }

    private String getRoundScore() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < teams.size(); i++) {
            if (i > 0) sb.append(" - ");
            sb.append(teams.get(i).getName()).append(": ").append(teams.get(i).getRoundWins());
        }
        return sb.toString();
    }

    // ===== Getters =====

    public UUID getMatchId() { return matchId; }
    public MatchConfig getConfig() { return config; }
    public List<Team> getTeams() { return teams; }
    public MatchState getState() { return state; }
    public int getCurrentRound() { return currentRound; }
    public Set<UUID> getSpectators() { return spectators; }
    public Map<UUID, Integer> getPlayerTeamMap() { return playerTeamMap; }
    public Map<UUID, Integer> getKillCounts() { return killCounts; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }

    public boolean isParticipant(UUID uuid) { return playerTeamMap.containsKey(uuid); }
    public boolean isSpectator(UUID uuid) { return spectators.contains(uuid); }

    public Team getTeamOf(UUID uuid) {
        Integer idx = playerTeamMap.get(uuid);
        if (idx == null) return null;
        return teams.get(idx);
    }

    public void addSpectator(UUID uuid) {
        spectators.add(uuid);
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            p.setGameMode(GameMode.SPECTATOR);
            Location specSpawn = config.getArenaInstance() != null ? config.getArenaInstance().getSpectatorSpawn()
                    : (config.getArena() != null ? config.getArena().getSpectatorSpawn() : null);
            if (specSpawn != null) p.teleport(specSpawn);
        }
    }
}

