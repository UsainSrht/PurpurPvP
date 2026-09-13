package com.usainsrht.purpurpvp.world;

import com.usainsrht.purpurpvp.PurpurPvP;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.logging.Logger;

/**
 * Manages world resolution, strategies, and layout offsets for Lobby, Duel Arenas, and FFA Maps.
 *
 * Strategies:
 * - SAME_WORLD: Lobby, dynamic duel grid, and FFA maps exist within the same world
 *   (with safe coordinate offsets), completely eliminating client terrain loading screens.
 * - SEPARATE_WORLDS: Dedicated worlds for each module for servers that prefer isolated world files.
 */
public class PvPWorldManager {

    public enum Strategy {
        SAME_WORLD,
        SEPARATE_WORLDS
    }

    private final PurpurPvP plugin;
    private final Logger logger;

    private Strategy strategy;
    private String sharedWorldName;
    private String lobbyWorldName;
    private String duelWorldName;
    private int duelOriginX;
    private int duelOriginZ;
    private int duelGridSpacing;
    private String ffaWorldName;
    private int ffaOriginX;
    private int ffaOriginZ;

    public PvPWorldManager(PurpurPvP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        var cfg = plugin.getConfig();

        String stratStr = cfg.getString("world-settings.strategy", "SAME_WORLD").toUpperCase();
        try {
            this.strategy = Strategy.valueOf(stratStr);
        } catch (IllegalArgumentException e) {
            this.strategy = Strategy.SAME_WORLD;
        }

        this.sharedWorldName = cfg.getString("world-settings.shared-world", "world");

        this.lobbyWorldName = cfg.getString("world-settings.lobby.world", "world");
        this.duelWorldName = cfg.getString("world-settings.duels.world", "purpurpvp_duels");
        this.duelOriginX = cfg.getInt("world-settings.duels.origin-x", 10000);
        this.duelOriginZ = cfg.getInt("world-settings.duels.origin-z", 10000);
        this.duelGridSpacing = cfg.getInt("world-settings.duels.grid-spacing", cfg.getInt("arena.grid-spacing", 300));

        this.ffaWorldName = cfg.getString("world-settings.ffa.world", "purpurpvp_ffa");
        this.ffaOriginX = cfg.getInt("world-settings.ffa.origin-x", -10000);
        this.ffaOriginZ = cfg.getInt("world-settings.ffa.origin-z", -10000);

        logger.info("[PvPWorldManager] World Strategy: " + strategy + " (Shared: " + sharedWorldName + ")");

        // Pre-warm worlds
        if (strategy == Strategy.SAME_WORLD) {
            getOrCreateWorld(sharedWorldName);
        } else {
            getOrCreateWorld(lobbyWorldName);
            getOrCreateWorld(duelWorldName);
            getOrCreateWorld(ffaWorldName);
        }
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public boolean isSameWorld() {
        return strategy == Strategy.SAME_WORLD;
    }

    public String getLobbyWorldName() {
        return isSameWorld() ? sharedWorldName : lobbyWorldName;
    }

    public World getLobbyWorld() {
        return getOrCreateWorld(getLobbyWorldName());
    }

    public String getDuelWorldName() {
        return isSameWorld() ? sharedWorldName : duelWorldName;
    }

    public World getDuelWorld() {
        return getOrCreateWorld(getDuelWorldName());
    }

    public String getFfaWorldName() {
        return isSameWorld() ? sharedWorldName : ffaWorldName;
    }

    public World getFfaWorld() {
        return getOrCreateWorld(getFfaWorldName());
    }

    public int getDuelOriginX() {
        return isSameWorld() ? duelOriginX : 0;
    }

    public int getDuelOriginZ() {
        return isSameWorld() ? duelOriginZ : 0;
    }

    public int getDuelGridSpacing() {
        return duelGridSpacing;
    }

    public int getFfaOriginX() {
        return isSameWorld() ? ffaOriginX : 0;
    }

    public int getFfaOriginZ() {
        return isSameWorld() ? ffaOriginZ : 0;
    }

    /**
     * Retrieves or automatically initializes a void flat world if not already loaded.
     */
    public World getOrCreateWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            try {
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                world = creator.createWorld();
            } catch (Exception e) {
                logger.warning("[PvPWorldManager] Could not generate world '" + worldName + "': " + e.getMessage());
                if (!Bukkit.getWorlds().isEmpty()) {
                    world = Bukkit.getWorlds().getFirst();
                }
            }
        }

        if (world != null) {
            configureWorldRules(world);
        }
        return world;
    }

    public void configureWorldRules(World world) {
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
    }
}
