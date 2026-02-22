package com.usainsrht.purpurpvp.room.gui;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.room.Room;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Paginated GUI for selecting a map/arena with icons and descriptions.
 */
public class MapSelectionGUI {

    private final PurpurPvP plugin;

    public MapSelectionGUI(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Room room, Consumer<Void> onBack) {
        Arena currentArena = room.getMatchConfig().getArena();
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
        int rows = Math.min(6, Math.max(3, (arenas.size() + 9) / 9 + 1));

        var gui = Gui.of(rows)
                .title(Component.text("Select Map", NamedTextColor.DARK_GREEN))
                .statelessComponent(container -> {
                    // Random option slot 0
                    var randomBuilder = ItemBuilder.from(Material.COMPASS)
                            .name(Component.text("Random Map", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
                            .lore(Component.text("A random available arena", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    if (currentArena == null) {
                        randomBuilder.glow();
                    }
                    container.setItem(0, randomBuilder.asGuiItem((p, ctx) -> {
                        room.getMatchConfig().setArena(null);
                        open(p, room, onBack);
                    }));

                    int slot = 2;
                    for (Arena arena : arenas) {
                        boolean inUse = plugin.getArenaManager().isInUse(arena.getName());
                        boolean selected = currentArena != null && currentArena.getName().equals(arena.getName());

                        List<Component> lore = new ArrayList<>();
                        // Arena description lines
                        for (String line : arena.getDescription()) {
                            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                        }
                        lore.add(Component.empty());
                        lore.add(Component.text("Teams: " + arena.getMaxTeams(), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                        if (arena.getMinLevel() > 0) {
                            lore.add(Component.text("Min Level: " + arena.getMinLevel(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                        }
                        lore.add(Component.text(inUse ? "⚠ IN USE" : (selected ? "✔ SELECTED" : "Click to select"),
                                inUse ? NamedTextColor.RED : (selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW))
                                .decoration(TextDecoration.ITALIC, false));

                        var itemBuilder = ItemBuilder.from(arena.getIcon() != null ? arena.getIcon() : Material.GRASS_BLOCK)
                                .name(Component.text(arena.getName(), selected ? NamedTextColor.GREEN : (inUse ? NamedTextColor.RED : NamedTextColor.WHITE))
                                        .decoration(TextDecoration.ITALIC, false))
                                .lore(lore);

                        if (selected) {
                            itemBuilder.glow();
                        }

                        container.setItem(slot++, itemBuilder.asGuiItem((p, ctx) -> {
                            if (!inUse) {
                                room.getMatchConfig().setArena(arena);
                                open(p, room, onBack);
                            }
                        }));
                        if (slot >= rows * 9 - 9) break;
                    }

                    // Back button
                    int backSlot = rows * 9 - 5;
                    container.setItem(backSlot, ItemBuilder.from(Material.ARROW)
                            .name(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem((p, ctx) -> onBack.accept(null)));
                })
                .build();

        gui.open(player);
    }
}
