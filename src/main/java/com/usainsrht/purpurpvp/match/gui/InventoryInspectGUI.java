package com.usainsrht.purpurpvp.match.gui;

import com.usainsrht.purpurpvp.match.MatchInventorySnapshot;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only GUI displaying a player's final equipment, health, and stats from a concluded match.
 */
public class InventoryInspectGUI {

    public static void open(Player viewer, MatchInventorySnapshot snapshot) {
        var gui = Gui.of(6)
                .title(Component.text("Inventory: " + snapshot.getPlayerName(), NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    // Storage contents
                    ItemStack[] storage = snapshot.getContents();
                    for (int i = 0; i < storage.length && i < 36; i++) {
                        if (storage[i] != null && !storage[i].getType().isAir()) {
                            container.setItem(i, ItemBuilder.from(storage[i].clone()).asGuiItem());
                        }
                    }

                    // Separator row
                    for (int i = 36; i < 45; i++) {
                        container.setItem(i, ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
                                .name(Component.empty())
                                .asGuiItem());
                    }

                    // Armor
                    ItemStack[] armor = snapshot.getArmor();
                    if (armor != null) {
                        for (int i = 0; i < armor.length && i < 4; i++) {
                            if (armor[i] != null && !armor[i].getType().isAir()) {
                                container.setItem(45 + i, ItemBuilder.from(armor[i].clone()).asGuiItem());
                            }
                        }
                    }

                    // Offhand
                    if (snapshot.getOffhand() != null && !snapshot.getOffhand().getType().isAir()) {
                        container.setItem(49, ItemBuilder.from(snapshot.getOffhand().clone()).asGuiItem());
                    }

                    // Player health head
                    container.setItem(50, ItemBuilder.from(Material.PLAYER_HEAD)
                            .name(Component.text(snapshot.getPlayerName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                            .lore(
                                    Component.text("Health: " + String.format("%.1f", snapshot.getHealth()) + " ❤", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                                    Component.text("Kills: " + snapshot.getKills(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                            )
                            .asGuiItem());

                    // Food
                    container.setItem(51, ItemBuilder.from(Material.COOKED_BEEF)
                            .name(Component.text("Food Level: " + snapshot.getFood(), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem());

                    // Potion effects
                    List<Component> effectLore = new ArrayList<>();
                    for (PotionEffect pe : snapshot.getEffects()) {
                        effectLore.add(Component.text(
                                pe.getType().getKey().getKey() + " (Amp: " + (pe.getAmplifier() + 1) + ", " + (pe.getDuration() / 20) + "s)",
                                NamedTextColor.LIGHT_PURPLE
                        ).decoration(TextDecoration.ITALIC, false));
                    }
                    if (effectLore.isEmpty()) {
                        effectLore.add(Component.text("No active effects", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    }

                    container.setItem(52, ItemBuilder.from(Material.POTION)
                            .name(Component.text("Active Effects", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false))
                            .lore(effectLore)
                            .asGuiItem());

                    // Close button
                    container.setItem(53, ItemBuilder.from(Material.BARRIER)
                            .name(Component.text("Close", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem((p, ctx) -> p.closeInventory()));
                })
                .build();

        gui.open(viewer);
    }
}
