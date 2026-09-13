package com.usainsrht.purpurpvp.ffa;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a persistent or dynamic Free-For-All (FFA) arena.
 */
public class FfaArena {

    private final String name;
    private String world;
    private final List<Location> spawnPoints = new ArrayList<>();
    private String defaultKitName;
    private final Set<UUID> players = ConcurrentHashMap.newKeySet();

    public FfaArena(String name, String world) {
        this.name = name;
        this.world = world;
    }

    public String getName() {
        return name;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public List<Location> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    public void addSpawnPoint(Location loc) {
        spawnPoints.add(loc);
    }

    public void clearSpawns() {
        spawnPoints.clear();
    }

    public String getDefaultKitName() {
        return defaultKitName;
    }

    public void setDefaultKitName(String defaultKitName) {
        this.defaultKitName = defaultKitName;
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public boolean hasPlayer(UUID uuid) {
        return players.contains(uuid);
    }

    public void addPlayer(UUID uuid) {
        players.add(uuid);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public Location getRandomSpawn(Location fallback) {
        if (spawnPoints.isEmpty()) return fallback;
        int idx = ThreadLocalRandom.current().nextInt(spawnPoints.size());
        return spawnPoints.get(idx);
    }
}
