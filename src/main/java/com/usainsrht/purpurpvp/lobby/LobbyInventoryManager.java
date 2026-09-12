package com.usainsrht.purpurpvp.lobby;

import com.usainsrht.purpurpvp.PurpurPvP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player inventory backups and restorations.
 * Guarantees players never lose their survival or lobby items when participating in PvP matches.
 */
public class LobbyInventoryManager implements Listener {

    private final PurpurPvP plugin;
    private final Map<UUID, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();

    public LobbyInventoryManager(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Captures a snapshot of the player's inventory and stats before entering a match.
     */
    public void backup(Player player) {
        snapshots.put(player.getUniqueId(), new PlayerSnapshot(player));
        player.getInventory().clear();
    }

    /**
     * Restores a previously saved snapshot. If no snapshot exists, gives standard lobby items.
     */
    public void restore(Player player) {
        PlayerSnapshot snapshot = snapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restore(player);
            plugin.getMessageService().send(player, "lobby.inventory-restored");
        } else {
            // No custom survival snapshot found; provide lobby items if configured
            if (plugin.getLobbyItemManager() != null) {
                plugin.getLobbyItemManager().giveLobbyItems(player);
            }
        }
    }

    public boolean hasSnapshot(UUID uuid) {
        return snapshots.containsKey(uuid);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // If player previously disconnected inside a match, restore their saved snapshot
        if (snapshots.containsKey(player.getUniqueId())) {
            restore(player);
        } else {
            // Fresh join in lobby
            if (plugin.getLobbyItemManager() != null && !plugin.getMatchManager().isInMatch(player.getUniqueId())) {
                plugin.getLobbyItemManager().giveLobbyItems(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Keep snapshot in memory so when they rejoin they don't lose items
    }
}
