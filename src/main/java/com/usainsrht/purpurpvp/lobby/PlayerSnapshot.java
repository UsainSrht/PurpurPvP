package com.usainsrht.purpurpvp.lobby;

import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Immutable snapshot of a player's inventory and physical state before entering a match.
 * Used to restore player items and status cleanly upon match exit or lobby return.
 */
public class PlayerSnapshot {

    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final ItemStack offhand;
    private final double health;
    private final int foodLevel;
    private final float saturation;
    private final int level;
    private final float exp;
    private final GameMode gameMode;
    private final Collection<PotionEffect> effects;

    public PlayerSnapshot(Player player) {
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

        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        this.health = Math.min(player.getHealth(), maxHp);
        this.foodLevel = player.getFoodLevel();
        this.saturation = player.getSaturation();
        this.level = player.getLevel();
        this.exp = player.getExp();
        this.gameMode = player.getGameMode();
        this.effects = new ArrayList<>(player.getActivePotionEffects());
    }

    public void restore(Player player) {
        player.getInventory().clear();
        player.getInventory().setStorageContents(contents);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offhand);

        player.setGameMode(gameMode);
        var maxHealthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        player.setHealth(Math.min(health, maxHp));
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);

        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }

        player.updateInventory();
    }
}
