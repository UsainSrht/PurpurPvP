package com.usainsrht.purpurpvp.ffa;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.util.LocationSerializer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages Free-For-All (FFA) maps, player state, instant respawning, and persistence.
 * Respects PvPWorldManager to ensure FFA maps reside on the same world as the lobby (preventing loading screens).
 */
public class FfaManager implements Listener {

    private final PurpurPvP plugin;
    private final Logger logger;
    private final Map<String, FfaArena> arenas = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerArenaMap = new ConcurrentHashMap<>();
    private File file;
    private FileConfiguration config;

    public FfaManager(PurpurPvP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();
        file = new File(plugin.getDataFolder(), "ffa.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.warning("[FfaManager] Could not create ffa.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = config.getConfigurationSection("arenas");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection arenaSec = section.getConfigurationSection(key);
                if (arenaSec == null) continue;

                String worldName = arenaSec.getString("world");
                if (worldName == null || worldName.isEmpty()) {
                    worldName = plugin.getPvPWorldManager() != null
                            ? plugin.getPvPWorldManager().getFfaWorldName()
                            : "world";
                }

                FfaArena arena = new FfaArena(key, worldName);
                arena.setDefaultKitName(arenaSec.getString("default-kit", "default"));

                List<String> spawnList = arenaSec.getStringList("spawns");
                for (String s : spawnList) {
                    Location loc = LocationSerializer.deserialize(s);
                    if (loc != null) arena.addSpawnPoint(loc);
                }

                arenas.put(key.toLowerCase(), arena);
            }
        }

        // Create default FFA arena if none exists
        if (arenas.isEmpty()) {
            String defaultWorld = plugin.getPvPWorldManager() != null
                    ? plugin.getPvPWorldManager().getFfaWorldName()
                    : "world";
            FfaArena defaultArena = new FfaArena("classic", defaultWorld);
            int originX = plugin.getPvPWorldManager() != null ? plugin.getPvPWorldManager().getFfaOriginX() : -10000;
            int originZ = plugin.getPvPWorldManager() != null ? plugin.getPvPWorldManager().getFfaOriginZ() : -10000;

            World w = plugin.getPvPWorldManager() != null
                    ? plugin.getPvPWorldManager().getFfaWorld()
                    : Bukkit.getWorld(defaultWorld);
            if (w != null) {
                defaultArena.addSpawnPoint(new Location(w, originX + 0.5, 100.0, originZ + 0.5));
            }
            arenas.put("classic", defaultArena);
            saveArenas();
        }

        logger.info("[FfaManager] Loaded " + arenas.size() + " FFA arenas.");
    }

    public void saveArenas() {
        if (config == null || file == null) return;
        config.set("arenas", null);

        for (FfaArena arena : arenas.values()) {
            String path = "arenas." + arena.getName();
            config.set(path + ".world", arena.getWorld());
            config.set(path + ".default-kit", arena.getDefaultKitName());

            List<String> spawnList = new ArrayList<>();
            for (Location loc : arena.getSpawnPoints()) {
                spawnList.add(LocationSerializer.serialize(loc));
            }
            config.set(path + ".spawns", spawnList);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            logger.severe("[FfaManager] Failed to save ffa.yml: " + e.getMessage());
        }
    }

    public Collection<FfaArena> getAllArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    public FfaArena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public boolean isInFfa(UUID uuid) {
        return playerArenaMap.containsKey(uuid);
    }

    public FfaArena getPlayerArena(UUID uuid) {
        String name = playerArenaMap.get(uuid);
        return name != null ? arenas.get(name) : null;
    }

    public boolean joinFfa(Player player, String arenaName) {
        FfaArena arena = getArena(arenaName);
        if (arena == null) {
            arena = arenas.values().stream().findFirst().orElse(null);
        }
        if (arena == null) {
            plugin.getMessageService().send(player, "error.invalid-args", Placeholder.parsed("usage", "No FFA arena available"));
            return false;
        }

        // Leave any existing match / queue / party FFA
        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
        }

        playerArenaMap.put(player.getUniqueId(), arena.getName().toLowerCase());
        arena.addPlayer(player.getUniqueId());

        // Prepare player
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Apply kit
        Kit kit = null;
        if (arena.getDefaultKitName() != null) {
            kit = plugin.getKitManager().getGlobalKit(arena.getDefaultKitName());
        }
        if (kit == null) {
            var globalKits = plugin.getKitManager().getAllGlobalKits();
            if (!globalKits.isEmpty()) kit = globalKits.iterator().next();
        }

        if (kit != null) {
            kit.applyTo(player);
        } else {
            player.getInventory().clear();
        }

        // Teleport to random spawn (same world = seamless instant teleport!)
        Location spawn = arena.getRandomSpawn(player.getLocation());
        player.teleportAsync(spawn);

        plugin.getMessageService().send(player, "ffa.joined", Placeholder.parsed("arena", arena.getName()));
        return true;
    }

    public void leaveFfa(Player player) {
        String arenaName = playerArenaMap.remove(player.getUniqueId());
        if (arenaName != null) {
            FfaArena arena = arenas.get(arenaName);
            if (arena != null) arena.removePlayer(player.getUniqueId());
        }

        // Send back to lobby
        Location lobby = plugin.getLobbyLocation();
        if (lobby != null) {
            player.teleportAsync(lobby);
        }

        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        // Restore lobby items
        if (plugin.getLobbyItemManager() != null) {
            plugin.getLobbyItemManager().giveLobbyItems(player);
        }

        plugin.getMessageService().send(player, "ffa.left");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        if (!isInFfa(player.getUniqueId())) return;

        FfaArena arena = getPlayerArena(player.getUniqueId());
        if (arena == null) return;

        // Prevent item drops on ground in FFA
        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = player.getKiller();
        if (killer != null && !killer.getUniqueId().equals(player.getUniqueId())) {
            plugin.getRankManager().recordFfaKill(killer.getUniqueId(), player.getUniqueId());
            plugin.getMessageService().send(killer, "ffa.kill", Placeholder.parsed("victim", player.getName()));
            plugin.getMessageService().send(player, "ffa.killed-by", Placeholder.parsed("killer", killer.getName()));
        }

        // Auto respawn after tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.spigot().respawn();
            joinFfa(player, arena.getName());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isInFfa(player.getUniqueId())) {
            String arenaName = playerArenaMap.remove(player.getUniqueId());
            if (arenaName != null) {
                FfaArena arena = arenas.get(arenaName);
                if (arena != null) arena.removePlayer(player.getUniqueId());
            }
        }
    }
}
