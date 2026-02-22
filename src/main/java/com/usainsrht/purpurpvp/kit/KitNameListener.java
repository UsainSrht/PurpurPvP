package com.usainsrht.purpurpvp.kit;

import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Uses AnvilGUI to prompt players for kit names via an anvil rename interface.
 */
public class KitNameListener {

    private final Plugin plugin;

    public KitNameListener(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens an anvil GUI for the player to type a kit name.
     * The callback receives the entered name on completion.
     */
    public void waitForInput(Player player, Consumer<String> callback) {
        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        meta.displayName(Component.text("Kit Name", NamedTextColor.GRAY));
        paper.setItemMeta(meta);

        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Enter Kit Name")
                .itemLeft(paper)
                .text("Kit Name")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    String name = stateSnapshot.getText().trim();
                    if (name.isEmpty()) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText("Kit Name"));
                    }
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .onClose(stateSnapshot -> {
                    String name = stateSnapshot.getText().trim();
                    if (!name.isEmpty() && !name.equals("Kit Name")) {
                        callback.accept(name);
                    }
                })
                .open(player);
    }
}

