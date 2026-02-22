package com.usainsrht.purpurpvp.arena;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a PvP arena with team spawn points, spectator spawn, icon, and description.
 */
public class Arena {

    private final String name;
    private final List<List<Location>> teamSpawns;
    private Location spectatorSpawn;
    private Location worldBorderCenter;
    private double worldBorderInitialSize = 200.0;
    private String world;
    private int minLevel;
    private Material icon = Material.GRASS_BLOCK;
    private List<String> description = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
        this.teamSpawns = new ArrayList<>();
        this.minLevel = 0;
    }

    public String getName() { return name; }

    public List<List<Location>> getTeamSpawns() { return teamSpawns; }

    public void setTeamSpawn(int teamIndex, List<Location> spawns) {
        while (teamSpawns.size() <= teamIndex) teamSpawns.add(new ArrayList<>());
        teamSpawns.set(teamIndex, spawns);
    }

    public void addTeamSpawnPoint(int teamIndex, Location loc) {
        while (teamSpawns.size() <= teamIndex) teamSpawns.add(new ArrayList<>());
        teamSpawns.get(teamIndex).add(loc);
    }

    public int getMaxTeams() { return teamSpawns.size(); }

    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public void setSpectatorSpawn(Location spectatorSpawn) { this.spectatorSpawn = spectatorSpawn; }

    public Location getWorldBorderCenter() { return worldBorderCenter; }
    public void setWorldBorderCenter(Location center) { this.worldBorderCenter = center; }

    public double getWorldBorderInitialSize() { return worldBorderInitialSize; }
    public void setWorldBorderInitialSize(double size) { this.worldBorderInitialSize = size; }

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public int getMinLevel() { return minLevel; }
    public void setMinLevel(int minLevel) { this.minLevel = minLevel; }

    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }

    public List<String> getDescription() { return description; }
    public void setDescription(List<String> description) { this.description = description != null ? description : new ArrayList<>(); }
}

