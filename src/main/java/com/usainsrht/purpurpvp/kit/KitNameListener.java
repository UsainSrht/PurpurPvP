package com.usainsrht.purpurpvp.kit;

import com.usainsrht.purpurpvp.PurpurPvP;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * Prompts players for kit names via Paper's native client-side Dialog API.
 */
public class KitNameListener {

    private final PurpurPvP plugin;

    public KitNameListener(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens a native Dialog prompt for the player to enter a kit name.
     * The callback receives the entered name on completion.
     */
    public void waitForInput(Player player, Consumer<String> callback) {
        Component title = plugin.getMessageService().toComponent("kit.dialog-title");
        if (title == null || title.equals(Component.empty())) {
            title = plugin.getMessageService().toComponent("kit.anvil-title");
        }
        if (title == null || title.equals(Component.empty())) {
            title = Component.text("Enter Kit Name");
        }

        Component prompt = plugin.getMessageService().toComponent("kit.dialog-prompt");
        if (prompt == null || prompt.equals(Component.empty())) {
            prompt = Component.text("Please enter a name for your kit:");
        }

        plugin.getDialogService().promptText(player, title, prompt, "", callback);
    }
}
