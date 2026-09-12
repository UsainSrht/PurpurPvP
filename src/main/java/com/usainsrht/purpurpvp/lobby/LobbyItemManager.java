package com.usainsrht.purpurpvp.lobby;

import com.usainsrht.purpurpvp.PurpurPvP;
import me.usainsrht.itemapi.yamlitem.YamlItem;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages configurable lobby hotbar items parsed directly from config.yml using ItemAPI (YamlItem).
 */
public class LobbyItemManager implements Listener {

    private final PurpurPvP plugin;
    private final NamespacedKey actionKey;
    private final Map<Integer, ItemStack> lobbyItems = new HashMap<>();
    private final Map<UUID, Long> clickCooldown = new ConcurrentHashMap<>();

    public LobbyItemManager(PurpurPvP plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "lobby_action");
        loadItems();
    }

    public void loadItems() {
        lobbyItems.clear();
        if (!plugin.getConfigManager().isLobbyItemsEnabled()) return;

        ConfigurationSection section = plugin.getConfigManager().getLobbyItemsSection();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemConfig = section.getConfigurationSection(key);
            if (itemConfig == null) continue;

            int slot = itemConfig.getInt("slot", 0);
            String action = itemConfig.getString("action", "").toUpperCase();
            ConfigurationSection itemData = itemConfig.getConfigurationSection("item");

            if (itemData != null && !action.isEmpty()) {
                ItemStack item = YamlItem.parse(itemData);
                if (item != null) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
                        item.setItemMeta(meta);
                    }
                    lobbyItems.put(slot, item);
                }
            }
        }
    }

    /**
     * Equips the player with standard lobby items.
     */
    public void giveLobbyItems(Player player) {
        if (!plugin.getConfigManager().isLobbyItemsEnabled()) return;
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;
        if (plugin.getRoomManager().isInRoom(player.getUniqueId())) return;

        player.getInventory().clear();
        for (Map.Entry<Integer, ItemStack> entry : lobbyItems.entrySet()) {
            player.getInventory().setItem(entry.getKey(), entry.getValue().clone());
        }
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;

        String action = item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = clickCooldown.get(player.getUniqueId());
        if (last != null && (now - last) < 500) return;
        clickCooldown.put(player.getUniqueId(), now);

        switch (action) {
            case "QUEUE" -> {
                // Open room browser or dispatch queue command
                plugin.getRoomBrowserGUI().open(player);
            }
            case "ROOMS" -> plugin.getRoomBrowserGUI().open(player);
            case "KITEDITOR" -> plugin.getKitEditorGUI().openKitList(player);
            case "LEADERBOARD" -> plugin.getLeaderboardGUI().open(player);
            case "PARTY" -> {
                if (plugin.getPartyManager().hasParty(player.getUniqueId())) {
                    plugin.getPartyManager().sendPartyInfo(player);
                } else {
                    plugin.getMessageService().send(player, "party.not-in-party");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
        }
    }
}
