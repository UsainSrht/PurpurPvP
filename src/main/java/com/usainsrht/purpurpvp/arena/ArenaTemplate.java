package com.usainsrht.purpurpvp.arena;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Blueprint for dynamically instantiating arenas on an automated grid.
 * Decouples match logic from fixed world coordinates to enable unlimited concurrent matches.
 */
public class ArenaTemplate {

    private final String name;
    private int minLevel;
    private double worldBorderInitialSize = 60.0;
    private Vector worldBorderCenterOffset = new Vector(0, 0, 0);

    // Relative offsets from the instance origin (X=0, Y=0, Z=0)
    private final List<List<Vector>> relativeTeamSpawns = new ArrayList<>();
    private final List<List<Float>> relativeTeamYaws = new ArrayList<>();
    private Vector relativeSpectatorSpawn = new Vector(0, 10, 0);
    private float spectatorYaw = 0f;

    // Bounding box offsets for cleanup
    private Vector minBound = new Vector(-40, -10, -40);
    private Vector maxBound = new Vector(40, 40, 40);

    public ArenaTemplate(String name) {
        this.name = name;
        // Default standard 2-team spawns
        List<Vector> t0 = List.of(new Vector(-15, 1, 0));
        List<Float> t0Yaw = List.of(-90f);
        List<Vector> t1 = List.of(new Vector(15, 1, 0));
        List<Float> t1Yaw = List.of(90f);

        relativeTeamSpawns.add(t0);
        relativeTeamYaws.add(t0Yaw);
        relativeTeamSpawns.add(t1);
        relativeTeamYaws.add(t1Yaw);
    }

    public static ArenaTemplate fromLegacyArena(Arena arena) {
        ArenaTemplate template = new ArenaTemplate(arena.getName());
        template.setMinLevel(arena.getMinLevel());
        template.setWorldBorderInitialSize(arena.getWorldBorderInitialSize());

        // Calculate center reference from spectator or team 0 spawn
        Location ref = arena.getSpectatorSpawn();
        if (ref == null && !arena.getTeamSpawns().isEmpty() && !arena.getTeamSpawns().getFirst().isEmpty()) {
            ref = arena.getTeamSpawns().getFirst().getFirst();
        }

        if (ref != null) {
            template.relativeTeamSpawns.clear();
            template.relativeTeamYaws.clear();

            for (List<Location> spawns : arena.getTeamSpawns()) {
                List<Vector> teamVecs = new ArrayList<>();
                List<Float> teamYaws = new ArrayList<>();
                for (Location loc : spawns) {
                    teamVecs.add(new Vector(loc.getX() - ref.getX(), loc.getY() - ref.getY(), loc.getZ() - ref.getZ()));
                    teamYaws.add(loc.getYaw());
                }
                template.relativeTeamSpawns.add(teamVecs);
                template.relativeTeamYaws.add(teamYaws);
            }

            if (arena.getSpectatorSpawn() != null) {
                Location spec = arena.getSpectatorSpawn();
                template.setRelativeSpectatorSpawn(new Vector(
                        spec.getX() - ref.getX(), spec.getY() - ref.getY(), spec.getZ() - ref.getZ()), spec.getYaw());
            }
        }

        return template;
    }

    public String getName() { return name; }
    public int getMinLevel() { return minLevel; }
    public void setMinLevel(int minLevel) { this.minLevel = minLevel; }
    public double getWorldBorderInitialSize() { return worldBorderInitialSize; }
    public void setWorldBorderInitialSize(double size) { this.worldBorderInitialSize = size; }
    public Vector getWorldBorderCenterOffset() { return worldBorderCenterOffset; }
    public void setWorldBorderCenterOffset(Vector offset) { this.worldBorderCenterOffset = offset; }

    public List<List<Vector>> getRelativeTeamSpawns() { return relativeTeamSpawns; }
    public List<List<Float>> getRelativeTeamYaws() { return relativeTeamYaws; }
    public Vector getRelativeSpectatorSpawn() { return relativeSpectatorSpawn; }
    public float getSpectatorYaw() { return spectatorYaw; }
    public void setRelativeSpectatorSpawn(Vector vec, float yaw) {
        this.relativeSpectatorSpawn = vec;
        this.spectatorYaw = yaw;
    }

    public Vector getMinBound() { return minBound; }
    public void setMinBound(Vector minBound) { this.minBound = minBound; }
    public Vector getMaxBound() { return maxBound; }
    public void setMaxBound(Vector maxBound) { this.maxBound = maxBound; }
}
