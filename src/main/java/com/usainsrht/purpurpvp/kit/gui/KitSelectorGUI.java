package com.usainsrht.purpurpvp.kit.gui;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.kit.KitManager;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * GUI for selecting a kit before a match.
 */
public class KitSelectorGUI {

    private final PurpurPvP plugin;
    private final KitManager kitManager;

    public KitSelectorGUI(PurpurPvP plugin) {
        this.plugin = plugin;
        this.kitManager = plugin.getKitManager();
    }

    /**
     * Opens kit selection GUI. Callback receives the selected kit.
     */
    public void open(Player player, Consumer<Kit> onSelect) {
        List<Kit> allKits = new ArrayList<>();

        // Add global kits
        for (Kit kit : kitManager.getAllGlobalKits()) {
            if (kit.getPermission() == null || player.hasPermission(kit.getPermission())) {
                allKits.add(kit);
            }
        }

        // Add player custom kits
        allKits.addAll(kitManager.getPlayerKits(player.getUniqueId()));

        var gui = Gui.of(4)
                .title(Component.text("Select a Kit", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    int slot = 0;
                    for (Kit kit : allKits) {
                        ItemStack icon = kit.getIcon() != null ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
                        container.setItem(slot++, ItemBuilder.from(icon)
                                .name(Component.text(kit.getName(), kit.isGlobal() ? NamedTextColor.GOLD : NamedTextColor.GREEN))
                                .lore(
                                        Component.text(kit.isGlobal() ? "Global Kit" : "Custom Kit",
                                                kit.isGlobal() ? NamedTextColor.YELLOW : NamedTextColor.AQUA),
                                        Component.text("Click to select", NamedTextColor.GRAY)
                                )
                                .asGuiItem((p, ctx) -> {
                                    p.closeInventory();
                                    onSelect.accept(kit);
                                }));

                        if (slot >= 36) break;
                    }
                })
                .build();

        gui.open(player);
    }
}
