package com.usainsrht.purpurpvp.kit;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a kit loadout (custom or global).
 */
public class Kit {

    private String name;
    private ItemStack[] contents;  // main inventory (36 slots)
    private ItemStack[] armor;     // 4 armor slots
    private ItemStack offhand;
    private ItemStack icon;        // display icon for GUIs
    private String permission;     // required permission (global kits)
    private boolean global;        // true = admin kit, false = player kit

    public Kit(String name) {
        this.name = name;
        this.contents = new ItemStack[36];
        this.armor = new ItemStack[4];
        this.global = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ItemStack[] getContents() { return contents; }
    public void setContents(ItemStack[] contents) { this.contents = contents; }

    public ItemStack[] getArmor() { return armor; }
    public void setArmor(ItemStack[] armor) { this.armor = armor; }

    public ItemStack getOffhand() { return offhand; }
    public void setOffhand(ItemStack offhand) { this.offhand = offhand; }

    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public boolean isGlobal() { return global; }
    public void setGlobal(boolean global) { this.global = global; }

    public void applyTo(org.bukkit.entity.Player player) {
        player.getInventory().clear();
        if (contents != null) {
            for (int i = 0; i < contents.length && i < 36; i++) {
                if (contents[i] != null) {
                    player.getInventory().setItem(i, contents[i].clone());
                }
            }
        }
        if (armor != null) {
            player.getInventory().setArmorContents(armor.clone());
        }
        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand.clone());
        }
        player.updateInventory();
    }
}

