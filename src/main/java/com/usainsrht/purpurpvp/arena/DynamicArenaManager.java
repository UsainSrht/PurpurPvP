package com.usainsrht.purpurpvp.arena;

import com.usainsrht.purpurpvp.PurpurPvP;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Dynamic Arena Instancing Engine.
 * Allocates, pools, and recycles arena instances on an automated 2D grid in a dedicated void world.
 * Enables 100+ simultaneous players without arena starvation or coordinate collisions.
 */
public class DynamicArenaManager {

    private final PurpurPvP plugin;
    private final Logger logger;
    private final Map<String, ArenaTemplate> templates = new ConcurrentHashMap<>();
    private final Map<String, Queue<ArenaInstance>> availablePool = new ConcurrentHashMap<>();
    private final Map<UUID, ArenaInstance> activeInstances = new ConcurrentHashMap<>();
    private final AtomicInteger nextGridIndex = new AtomicInteger(0);

    private World arenaWorld;

    public DynamicArenaManager(PurpurPvP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        initWorld();
        loadTemplates();
    }

    private void initWorld() {
        if (plugin.getPvPWorldManager() != null) {
            arenaWorld = plugin.getPvPWorldManager().getDuelWorld();
            return;
        }

        String worldName = plugin.getConfigManager().getArenaWorld();
        arenaWorld = Bukkit.getWorld(worldName);

        if (arenaWorld == null) {
            try {
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
                arenaWorld = creator.createWorld();
            } catch (Exception e) {
                logger.warning("[DynamicArena] Could not create void world '" + worldName + "'. Falling back to default world.");
                arenaWorld = Bukkit.getWorlds().getFirst();
            }
        }

        if (arenaWorld != null) {
            arenaWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            arenaWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            arenaWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        }
    }

    public void loadTemplates() {
        templates.clear();
        availablePool.clear();

        // Convert existing legacy arenas into dynamic templates
        for (Arena legacy : plugin.getArenaManager().getAllArenas()) {
            ArenaTemplate template = ArenaTemplate.fromLegacyArena(legacy);
            templates.put(template.getName().toLowerCase(), template);
            availablePool.put(template.getName().toLowerCase(), new ConcurrentLinkedQueue<>());
        }

        // Ensure at least one default template exists if none configured
        if (templates.isEmpty()) {
            ArenaTemplate defaultTemplate = new ArenaTemplate("default");
            templates.put("default", defaultTemplate);
            availablePool.put("default", new ConcurrentLinkedQueue<>());
        }

        logger.info("[DynamicArenaManager] Loaded " + templates.size() + " arena templates.");
    }

    /**
     * Acquires an arena instance for a match.
     * Uses pre-warmed instance from the pool or provisions a new grid slot dynamically.
     */
    public ArenaInstance acquireInstance(String templateName) {
        ArenaTemplate template = templates.get(templateName.toLowerCase());
        if (template == null) {
            template = templates.values().stream().findFirst().orElse(new ArenaTemplate("default"));
        }

        Queue<ArenaInstance> pool = availablePool.computeIfAbsent(template.getName().toLowerCase(), k -> new ConcurrentLinkedQueue<>());
        ArenaInstance instance = pool.poll();

        if (instance == null) {
            // Provision a new instance on the grid
            int index = nextGridIndex.getAndIncrement();
            int spacing = plugin.getPvPWorldManager() != null
                    ? plugin.getPvPWorldManager().getDuelGridSpacing()
                    : plugin.getConfigManager().getArenaGridSpacing();

            int originBaseX = plugin.getPvPWorldManager() != null ? plugin.getPvPWorldManager().getDuelOriginX() : 0;
            int originBaseZ = plugin.getPvPWorldManager() != null ? plugin.getPvPWorldManager().getDuelOriginZ() : 0;

            // 2D grid allocation
            int gridX = originBaseX + ((index % 20) * spacing);
            int gridZ = originBaseZ + ((index / 20) * spacing);

            World world = plugin.getPvPWorldManager() != null
                    ? plugin.getPvPWorldManager().getDuelWorld()
                    : (arenaWorld != null ? arenaWorld : Bukkit.getWorlds().getFirst());
            Location origin = new Location(world, gridX, 64, gridZ);

            instance = new ArenaInstance(template, index, origin);
        }

        instance.setInUse(true);
        activeInstances.put(instance.getInstanceId(), instance);
        return instance;
    }

    /**
     * Releases an arena instance, performing instant delta rollback and returning it to the pool.
     */
    public void releaseInstance(ArenaInstance instance) {
        if (instance == null) return;
        activeInstances.remove(instance.getInstanceId());

        // Fast rollback and cleanup
        instance.cleanAndReset();

        // Return to pool
        Queue<ArenaInstance> pool = availablePool.computeIfAbsent(instance.getTemplate().getName().toLowerCase(), k -> new ConcurrentLinkedQueue<>());
        pool.offer(instance);
    }

    public ArenaTemplate getTemplate(String name) {
        return templates.get(name.toLowerCase());
    }

    public Collection<ArenaTemplate> getAllTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public void shutdown() {
        for (ArenaInstance instance : activeInstances.values()) {
            instance.cleanAndReset();
        }
        activeInstances.clear();
        availablePool.clear();
    }
}
