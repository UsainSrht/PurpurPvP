package com.usainsrht.purpurpvp.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An active, isolated instance of an arena placed on the dynamic instancing grid.
 * Tracks local block changes and supports instant delta-rollback and entity cleanup.
 */
public class ArenaInstance {

    private final UUID instanceId;
    private final ArenaTemplate template;
    private final int gridIndex;
    private final Location origin;
    private final List<List<Location>> teamSpawns = new ArrayList<>();
    private final Location spectatorSpawn;
    private final Location worldBorderCenter;
    private final BoundingBox boundingBox;

    private boolean inUse;
    private final Map<Location, Material> originalBlocks = new ConcurrentHashMap<>();

    public ArenaInstance(ArenaTemplate template, int gridIndex, Location origin) {
        this.instanceId = UUID.randomUUID();
        this.template = template;
        this.gridIndex = gridIndex;
        this.origin = origin;
        this.inUse = false;

        World world = origin.getWorld();

        // Calculate absolute team spawns from relative vectors
        for (int t = 0; t < template.getRelativeTeamSpawns().size(); t++) {
            List<Vector> relSpawns = template.getRelativeTeamSpawns().get(t);
            List<Float> yaws = template.getRelativeTeamYaws().get(t);
            List<Location> spawns = new ArrayList<>();

            for (int i = 0; i < relSpawns.size(); i++) {
                Vector v = relSpawns.get(i);
                float yaw = i < yaws.size() ? yaws.get(i) : 0f;
                spawns.add(new Location(world, origin.getX() + v.getX(), origin.getY() + v.getY(), origin.getZ() + v.getZ(), yaw, 0f));
            }
            this.teamSpawns.add(spawns);
        }

        // Spectator spawn
        Vector s = template.getRelativeSpectatorSpawn();
        this.spectatorSpawn = new Location(world, origin.getX() + s.getX(), origin.getY() + s.getY(), origin.getZ() + s.getZ(), template.getSpectatorYaw(), 0f);

        // World border center
        Vector wb = template.getWorldBorderCenterOffset();
        this.worldBorderCenter = new Location(world, origin.getX() + wb.getX(), origin.getY() + wb.getY(), origin.getZ() + wb.getZ());

        // Bounding box for entity purging
        Vector min = template.getMinBound();
        Vector max = template.getMaxBound();
        this.boundingBox = new BoundingBox(
                origin.getX() + min.getX(), origin.getY() + min.getY(), origin.getZ() + min.getZ(),
                origin.getX() + max.getX(), origin.getY() + max.getY(), origin.getZ() + max.getZ()
        );
    }

    /**
     * Records a modified block for instant delta restoration.
     */
    public void trackBlockChange(Location loc, Material original) {
        originalBlocks.putIfAbsent(loc.clone(), original);
    }

    /**
     * Fast rollback: restores only modified blocks and clears dropped entities and projectiles.
     */
    public void cleanAndReset() {
        // 1. Restore modified blocks
        for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue(), false);
        }
        originalBlocks.clear();

        // 2. Clear entities in bounds (items, arrows, TNT, end crystals, etc.)
        World world = origin.getWorld();
        if (world != null) {
            for (Entity entity : world.getNearbyEntities(boundingBox)) {
                if (entity instanceof Item || entity instanceof Projectile || entity instanceof TNTPrimed
                        || entity instanceof EnderCrystal || entity instanceof AreaEffectCloud) {
                    entity.remove();
                }
            }
        }

        this.inUse = false;
    }

    public UUID getInstanceId() { return instanceId; }
    public ArenaTemplate getTemplate() { return template; }
    public int getGridIndex() { return gridIndex; }
    public Location getOrigin() { return origin; }
    public List<List<Location>> getTeamSpawns() { return teamSpawns; }
    public Location getSpectatorSpawn() { return spectatorSpawn; }
    public Location getWorldBorderCenter() { return worldBorderCenter; }
    public BoundingBox getBoundingBox() { return boundingBox; }

    public boolean isInUse() { return inUse; }
    public void setInUse(boolean inUse) { this.inUse = inUse; }
}
