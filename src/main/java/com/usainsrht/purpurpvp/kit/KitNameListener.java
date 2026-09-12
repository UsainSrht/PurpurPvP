package com.usainsrht.purpurpvp.kit;

import com.usainsrht.purpurpvp.PurpurPvP;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Uses AnvilGUI to prompt players for kit names via an anvil rename interface.
 */
public class KitNameListener {

    private final PurpurPvP plugin;

    public KitNameListener(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens an anvil GUI for the player to type a kit name.
     * The callback receives the entered name on completion.
     */
    public void waitForInput(Player player, Consumer<String> callback) {
        String title = PlainTextComponentSerializer.plainText().serialize(plugin.getMessageService().toComponent("kit.anvil-title"));
        String placeholder = PlainTextComponentSerializer.plainText().serialize(plugin.getMessageService().toComponent("kit.anvil-placeholder"));
        if (title.isEmpty()) title = "Enter Kit Name";
        if (placeholder.isEmpty()) placeholder = "Kit Name";

        ItemStack paper = new ItemStack(Material.PAPER);
        ItemMeta meta = paper.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.getMessageService().toComponent("kit.anvil-placeholder"));
            paper.setItemMeta(meta);
        }

        final String finalPlaceholder = placeholder;
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title(title)
                .itemLeft(paper)
                .text(finalPlaceholder)
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    String name = stateSnapshot.getText().trim();
                    if (name.isEmpty()) {
                        return List.of(AnvilGUI.ResponseAction.replaceInputText(finalPlaceholder));
                    }
                    return List.of(AnvilGUI.ResponseAction.close());
                })
                .onClose(stateSnapshot -> {
                    String name = stateSnapshot.getText().trim();
                    if (!name.isEmpty() && !name.equals(finalPlaceholder)) {
                        callback.accept(name);
                    }
                })
                .open(player);
    }
}

