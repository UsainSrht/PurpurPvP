package com.usainsrht.purpurpvp.room.gui;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.room.Room;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * GUI to browse and join open rooms.
 */
public class RoomBrowserGUI {

    private final PurpurPvP plugin;

    public RoomBrowserGUI(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Collection<Room> publicRooms = plugin.getRoomManager().getPublicRooms();

        var gui = Gui.of(6)
                .title(Component.text("Room Browser", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    if (publicRooms.isEmpty()) {
                        container.setItem(22, ItemBuilder.from(Material.BARRIER)
                                .name(Component.text("No rooms available", NamedTextColor.RED))
                                .lore(Component.text("Create one with /duel or the sign!", NamedTextColor.GRAY))
                                .asGuiItem());
                    }

                    int slot = 0;
                    for (Room room : publicRooms) {
                        Player host = Bukkit.getPlayer(room.getHostUuid());
                        String hostName = host != null ? host.getName() : "Unknown";

                        var skullBuilder = ItemBuilder.skull();
                        if (host != null) {
                            skullBuilder.owner(host);
                        }
                        container.setItem(slot++, skullBuilder
                                .name(Component.text(hostName + "'s Room", NamedTextColor.GOLD))
                                .lore(
                                        Component.text("Kit: " + (room.getMatchConfig().getKit() != null
                                                ? room.getMatchConfig().getKit().getName() : "Not set"), NamedTextColor.GRAY),
                                        Component.text("Mode: " + room.getMatchConfig().getTeamSize() + "v"
                                                + room.getMatchConfig().getTeamSize(), NamedTextColor.GRAY),
                                        Component.text("Players: " + room.getPlayerCount() + "/"
                                                + room.getMaxPlayers(), NamedTextColor.AQUA),
                                        Component.text("Best of: " + room.getMatchConfig().getBestOf(), NamedTextColor.GRAY),
                                        Component.empty(),
                                        Component.text("Click to join!", NamedTextColor.GREEN)
                                )
                                .asGuiItem((p, ctx) -> {
                                    p.closeInventory();
                                    boolean joined = plugin.getRoomManager().joinRoom(p.getUniqueId(), room.getRoomId());
                                    if (!joined) {
                                        plugin.getMessageService().send(p, "room.full");
                                    } else {
                                        plugin.getMessageService().send(p, "room.joined",
                                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", hostName));
                                    }
                                }));

                        if (slot >= 45) break;
                    }

                    // Create Room button
                    container.setItem(49, ItemBuilder.from(Material.EMERALD)
                            .name(Component.text("Create New Room", NamedTextColor.GREEN))
                            .lore(Component.text("Click to create your own room", NamedTextColor.GRAY))
                            .asGuiItem((p, ctx) -> {
                                p.closeInventory();
                                Room newRoom = plugin.getRoomManager().createRoom(p.getUniqueId());
                                if (newRoom != null) {
                                    plugin.getMessageService().send(p, "room.created");
                                    plugin.getRoomCreationGUI().open(p, newRoom);
                                } else {
                                    plugin.getMessageService().send(p, "error.already-in-match");
                                }
                            }));

                    // Refresh button
                    container.setItem(53, ItemBuilder.from(Material.CLOCK)
                            .name(Component.text("Refresh", NamedTextColor.YELLOW))
                            .asGuiItem((p, ctx) -> open(p)));
                })
                .build();

        gui.open(player);
    }
}

