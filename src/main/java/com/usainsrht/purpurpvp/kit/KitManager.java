package com.usainsrht.purpurpvp.kit;

import com.usainsrht.purpurpvp.database.repository.KitRepository;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages global and player-custom kits.
 */
public class KitManager {

    private final KitRepository kitRepo;
    private final Logger logger;

    // Global kits (admin-defined)
    private final Map<String, Kit> globalKits = new ConcurrentHashMap<>();

    // Cache of player custom kits: UUID -> (kitName -> Kit)
    private final Map<UUID, Map<String, Kit>> playerKitCache = new ConcurrentHashMap<>();

    public KitManager(KitRepository kitRepo, Logger logger) {
        this.kitRepo = kitRepo;
        this.logger = logger;
        loadGlobalKits();
    }

    // ===== Global Kits =====

    public void loadGlobalKits() {
        globalKits.clear();
        for (Kit kit : kitRepo.getGlobalKits()) {
            globalKits.put(kit.getName().toLowerCase(), kit);
        }
        logger.info("[KitManager] Loaded " + globalKits.size() + " global kits.");
    }

    public void saveGlobalKit(Kit kit) {
        kit.setGlobal(true);
        kitRepo.saveGlobalKit(kit);
        globalKits.put(kit.getName().toLowerCase(), kit);
    }

    public void deleteGlobalKit(String name) {
        kitRepo.deleteGlobalKit(name);
        globalKits.remove(name.toLowerCase());
    }

    public Kit getGlobalKit(String name) {
        return globalKits.get(name.toLowerCase());
    }

    public Collection<Kit> getAllGlobalKits() {
        return Collections.unmodifiableCollection(globalKits.values());
    }

    // ===== Player Kits =====

    public void loadPlayerKits(UUID uuid) {
        Map<String, Kit> kits = new HashMap<>();
        for (Kit kit : kitRepo.getPlayerKits(uuid)) {
            kits.put(kit.getName().toLowerCase(), kit);
        }
        playerKitCache.put(uuid, kits);
    }

    public void unloadPlayerKits(UUID uuid) {
        playerKitCache.remove(uuid);
    }

    public List<Kit> getPlayerKits(UUID uuid) {
        Map<String, Kit> kits = playerKitCache.get(uuid);
        if (kits == null) return Collections.emptyList();
        return new ArrayList<>(kits.values());
    }

    public Kit getPlayerKit(UUID uuid, String name) {
        Map<String, Kit> kits = playerKitCache.get(uuid);
        if (kits == null) return null;
        return kits.get(name.toLowerCase());
    }

    public void savePlayerKit(UUID uuid, Kit kit) {
        kitRepo.savePlayerKit(uuid, kit);
        playerKitCache.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(kit.getName().toLowerCase(), kit);
    }

    public void deletePlayerKit(UUID uuid, String name) {
        kitRepo.deletePlayerKit(uuid, name);
        Map<String, Kit> kits = playerKitCache.get(uuid);
        if (kits != null) kits.remove(name.toLowerCase());
    }

    // ===== Kit Application =====

    /**
     * Applies a kit to a player (clears inventory first).
     */
    public void applyKit(Player player, Kit kit) {
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setContents(kit.getContents());
        inv.setArmorContents(kit.getArmor());
        if (kit.getOffhand() != null) {
            inv.setItemInOffHand(kit.getOffhand());
        }
        player.updateInventory();
    }

    /**
     * Creates a kit from the player's current inventory.
     */
    public Kit createFromInventory(Player player, String name) {
        Kit kit = new Kit(name);
        PlayerInventory inv = player.getInventory();

        ItemStack[] storage = inv.getStorageContents();
        kit.setContents(Arrays.copyOf(storage, 36));
        kit.setArmor(inv.getArmorContents().clone());
        kit.setOffhand(inv.getItemInOffHand().getType().isAir() ? null : inv.getItemInOffHand().clone());

        return kit;
    }
}

