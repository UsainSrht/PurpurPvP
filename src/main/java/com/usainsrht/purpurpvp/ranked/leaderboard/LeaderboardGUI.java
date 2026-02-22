package com.usainsrht.purpurpvp.ranked.leaderboard;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * GUI to display top players by Elo rating.
 */
public class LeaderboardGUI {

    private final PurpurPvP plugin;

    public LeaderboardGUI(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        // Load top players async, then populate on main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerProfile> topElo = plugin.getRankManager().getTopByElo(45);

            Bukkit.getScheduler().runTask(plugin, () -> {
                var gui = Gui.of(6)
                        .title(Component.text("Leaderboard", NamedTextColor.GOLD))
                        .statelessComponent(container -> {
                            int slot = 0;
                            int rank = 1;
                            for (PlayerProfile profile : topElo) {
                                NamedTextColor rankColor = switch (rank) {
                                    case 1 -> NamedTextColor.GOLD;
                                    case 2 -> NamedTextColor.GRAY;
                                    case 3 -> NamedTextColor.RED;
                                    default -> NamedTextColor.WHITE;
                                };

                                container.setItem(slot++, ItemBuilder.skull()
                                        .owner(Bukkit.getOfflinePlayer(profile.getUuid()))
                                        .name(Component.text("#" + rank + " " + profile.getUsername(), rankColor)
                                                .decoration(TextDecoration.ITALIC, false))
                                        .lore(
                                                Component.text("Elo: " + String.format("%.0f", profile.getEloRating()), NamedTextColor.YELLOW)
                                                        .decoration(TextDecoration.ITALIC, false),
                                                Component.text("Level: " + profile.getLevel(), NamedTextColor.AQUA)
                                                        .decoration(TextDecoration.ITALIC, false),
                                                Component.text("W/L: " + profile.getWins() + "/" + profile.getLosses(), NamedTextColor.GREEN)
                                                        .decoration(TextDecoration.ITALIC, false),
                                                Component.text("K/D: " + profile.getKills() + "/" + profile.getDeaths(), NamedTextColor.GRAY)
                                                        .decoration(TextDecoration.ITALIC, false)
                                        )
                                        .asGuiItem());
                                rank++;
                                if (slot >= 45) break;
                            }

                            // Player's own rank indicator
                            PlayerProfile own = plugin.getRankManager().getProfile(player.getUniqueId());
                            if (own != null) {
                                container.setItem(49, ItemBuilder.from(Material.NETHER_STAR)
                                        .name(Component.text("Your Stats", NamedTextColor.GREEN)
                                                .decoration(TextDecoration.ITALIC, false))
                                        .lore(
                                                Component.text("Elo: " + String.format("%.0f", own.getEloRating()), NamedTextColor.YELLOW)
                                                        .decoration(TextDecoration.ITALIC, false),
                                                Component.text("Level: " + own.getLevel(), NamedTextColor.AQUA)
                                                        .decoration(TextDecoration.ITALIC, false),
                                                Component.text("W/L: " + own.getWins() + "/" + own.getLosses(), NamedTextColor.GREEN)
                                                        .decoration(TextDecoration.ITALIC, false)
                                        )
                                        .asGuiItem());
                            }
                        })
                        .build();

                gui.open(player);
            });
        });
    }
}
