package com.usainsrht.purpurpvp.arena;

import com.usainsrht.purpurpvp.util.LocationSerializer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages arenas: CRUD, persistence via arenas.yml, and availability tracking.
 */
public class ArenaManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private final Set<String> inUse = ConcurrentHashMap.newKeySet();
    private File arenasFile;
    private FileConfiguration arenasConfig;

    public ArenaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            try { arenasFile.createNewFile(); } catch (IOException e) {
                logger.severe("[ArenaManager] Could not create arenas.yml");
            }
        }
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);

        ConfigurationSection section = arenasConfig.getConfigurationSection("arenas");
        if (section == null) return;

        for (String name : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(name);
            if (arenaSection == null) continue;

            Arena arena = new Arena(name);
            arena.setWorld(arenaSection.getString("world", "world"));
            arena.setMinLevel(arenaSection.getInt("min-level", 0));

            String specSpawn = arenaSection.getString("spectator-spawn");
            if (specSpawn != null) arena.setSpectatorSpawn(LocationSerializer.deserialize(specSpawn));

            ConfigurationSection teamsSection = arenaSection.getConfigurationSection("teams");
            if (teamsSection != null) {
                for (String teamKey : teamsSection.getKeys(false)) {
                    int teamIndex = Integer.parseInt(teamKey);
                    List<String> spawnStrings = teamsSection.getStringList(teamKey);
                    List<Location> spawns = new ArrayList<>();
                    for (String s : spawnStrings) {
                        Location loc = LocationSerializer.deserialize(s);
                        if (loc != null) spawns.add(loc);
                    }
                    arena.setTeamSpawn(teamIndex, spawns);
                }
            }

            arenas.put(name.toLowerCase(), arena);
        }
        logger.info("[ArenaManager] Loaded " + arenas.size() + " arenas.");
    }

    public void saveArenas() {
        arenasConfig.set("arenas", null);
        for (Arena arena : arenas.values()) {
            String path = "arenas." + arena.getName();
            arenasConfig.set(path + ".world", arena.getWorld());
            arenasConfig.set(path + ".min-level", arena.getMinLevel());
            if (arena.getSpectatorSpawn() != null) {
                arenasConfig.set(path + ".spectator-spawn",
                        LocationSerializer.serialize(arena.getSpectatorSpawn()));
            }
            for (int i = 0; i < arena.getTeamSpawns().size(); i++) {
                List<String> spawnStrings = new ArrayList<>();
                for (Location loc : arena.getTeamSpawns().get(i)) {
                    spawnStrings.add(LocationSerializer.serialize(loc));
                }
                arenasConfig.set(path + ".teams." + i, spawnStrings);
            }
        }
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            logger.severe("[ArenaManager] Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public Arena createArena(String name) {
        Arena arena = new Arena(name);
        arenas.put(name.toLowerCase(), arena);
        saveArenas();
        return arena;
    }

    public void deleteArena(String name) {
        arenas.remove(name.toLowerCase());
        saveArenas();
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (!inUse.contains(arena.getName().toLowerCase())) {
                return arena;
            }
        }
        return null;
    }

    public Arena getAvailableArena(int requiredTeams) {
        for (Arena arena : arenas.values()) {
            if (!inUse.contains(arena.getName().toLowerCase()) && arena.getMaxTeams() >= requiredTeams) {
                return arena;
            }
        }
        return null;
    }

    public void markInUse(String arenaName) {
        inUse.add(arenaName.toLowerCase());
    }

    public void markAvailable(String arenaName) {
        inUse.remove(arenaName.toLowerCase());
    }

    public boolean isInUse(String arenaName) {
        return inUse.contains(arenaName.toLowerCase());
    }
}

