package com.usainsrht.purpurpvp.kit.gui;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.kit.KitManager;
import dev.triumphteam.gui.click.GuiClick;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI for creating and editing kits.
 */
public class KitEditorGUI {

    private final PurpurPvP plugin;
    private final KitManager kitManager;

    public KitEditorGUI(PurpurPvP plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
    }

    /**
     * Opens the kit editor for a player.
     * Shows their current kits and option to create new.
     */
    public void openKitList(Player player) {
        List<Kit> playerKits = kitManager.getPlayerKits(player.getUniqueId());
        List<Kit> globalKits = kitManager.getAllGlobalKits().stream().toList();

        var gui = Gui.of(6)
                .title(Component.text("Your Kits", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    int slot = 0;

                    // Global kits section
                    for (Kit kit : globalKits) {
                        if (kit.getPermission() != null && !player.hasPermission(kit.getPermission())) continue;
                        ItemStack icon = kit.getIcon() != null ? kit.getIcon().clone() : new ItemStack(Material.CHEST);
                        container.setItem(slot++, ItemBuilder.from(icon)
                                .name(Component.text(kit.getName(), NamedTextColor.GOLD))
                                .lore(
                                        Component.text("Global Kit", NamedTextColor.YELLOW),
                                        Component.text("Click to preview", NamedTextColor.GRAY)
                                )
                                .asGuiItem((p, ctx) -> openKitPreview(p, kit)));
                        if (slot >= 18) break;
                    }

                    // Separator
                    for (int i = 18; i < 27; i++) {
                        container.setItem(i, ItemBuilder.from(Material.PURPLE_STAINED_GLASS_PANE)
                                .name(Component.text(" "))
                                .asGuiItem());
                    }

                    // Player custom kits
                    int pSlot = 27;
                    for (Kit kit : playerKits) {
                        ItemStack icon = kit.getIcon() != null ? kit.getIcon().clone() : new ItemStack(Material.ENDER_CHEST);
                        container.setItem(pSlot++, ItemBuilder.from(icon)
                                .name(Component.text(kit.getName(), NamedTextColor.GREEN))
                                .lore(
                                        Component.text("Custom Kit", NamedTextColor.AQUA),
                                        Component.text("Left-click to preview", NamedTextColor.GRAY),
                                        Component.text("Right-click to delete", NamedTextColor.RED)
                                )
                                .asGuiItem((p, ctx) -> {
                                    if (ctx.guiClick() == GuiClick.RIGHT) {
                                        kitManager.deletePlayerKit(p.getUniqueId(), kit.getName());
                                        p.sendMessage(Component.text("Kit deleted: " + kit.getName(), NamedTextColor.RED));
                                        openKitList(p);
                                    } else {
                                        openKitPreview(p, kit);
                                    }
                                }));
                        if (pSlot >= 45) break;
                    }

                    // Create new kit button
                    container.setItem(49, ItemBuilder.from(Material.EMERALD)
                            .name(Component.text("Create Kit from Inventory", NamedTextColor.GREEN))
                            .lore(Component.text("Saves your current loadout", NamedTextColor.GRAY))
                            .asGuiItem((p, ctx) -> {
                                p.closeInventory();
                                plugin.getKitNameListener().waitForInput(p, name -> {
                                    Kit newKit = kitManager.createFromInventory(p, name);
                                    kitManager.savePlayerKit(p.getUniqueId(), newKit);
                                    p.sendMessage(Component.text("Kit saved: " + name, NamedTextColor.GREEN));
                                });
                            }));
                })
                .build();

        gui.open(player);
    }

    public void openKitPreview(Player player, Kit kit) {
        var gui = Gui.of(6)
                .title(Component.text("Kit: " + kit.getName(), NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    // Show kit contents
                    ItemStack[] contents = kit.getContents();
                    for (int i = 0; i < contents.length && i < 36; i++) {
                        if (contents[i] != null) {
                            container.setItem(i, ItemBuilder.from(contents[i].clone()).asGuiItem());
                        }
                    }

                    // Armor display row
                    ItemStack[] armor = kit.getArmor();
                    if (armor != null) {
                        for (int i = 0; i < armor.length && i < 4; i++) {
                            if (armor[i] != null) {
                                container.setItem(36 + i, ItemBuilder.from(armor[i].clone()).asGuiItem());
                            }
                        }
                    }

                    // Offhand
                    if (kit.getOffhand() != null) {
                        container.setItem(40, ItemBuilder.from(kit.getOffhand().clone()).asGuiItem());
                    }

                    // Back button
                    container.setItem(49, ItemBuilder.from(Material.ARROW)
                            .name(Component.text("Back", NamedTextColor.RED))
                            .asGuiItem((p, ctx) -> openKitList(p)));
                })
                .build();

        gui.open(player);
    }
}
