package com.usainsrht.purpurpvp.match;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Read-only snapshot of a player's exact equipment, health, food, and active potion effects
 * at the instant a match finishes. Displayed in post-match inspection GUIs.
 */
public class MatchInventorySnapshot {

    private final UUID playerUuid;
    private final String playerName;
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final double health;
    private final int food;
    private final Collection<PotionEffect> effects;
    private final int kills;

    public MatchInventorySnapshot(Player player, int kills) {
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();

        ItemStack[] invStorage = player.getInventory().getStorageContents();
        this.contents = new ItemStack[invStorage.length];
        for (int i = 0; i < invStorage.length; i++) {
            this.contents[i] = invStorage[i] != null ? invStorage[i].clone() : null;
        }

        ItemStack[] armorContents = player.getInventory().getArmorContents();
        this.armor = new ItemStack[armorContents.length];
        for (int i = 0; i < armorContents.length; i++) {
            this.armor[i] = armorContents[i] != null ? armorContents[i].clone() : null;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        this.offhand = off != null && !off.getType().isAir() ? off.clone() : null;

        this.health = player.getHealth();
        this.food = player.getFoodLevel();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
        this.kills = kills;
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public ItemStack[] getContents() { return contents; }
    public ItemStack[] getArmor() { return armor; }
    public ItemStack getOffhand() { return offhand; }
    public double getHealth() { return health; }
    public int getFood() { return food; }
    public Collection<PotionEffect> getEffects() { return effects; }
    public int getKills() { return kills; }
}
