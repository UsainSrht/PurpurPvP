package com.usainsrht.purpurpvp.room.gui;

import com.usainsrht.purpurpvp.match.GameRules;
import dev.triumphteam.gui.click.GuiClick;
import dev.triumphteam.gui.element.GuiItem;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Sub-GUI for world border settings:
 *   - Toggle on/off
 *   - Damage per second (+/-)
 *   - Close after X seconds (+/-)
 *   - Closing speed (+/-)
 */
public class WorldBorderSettingsGUI {

    public void open(Player player, GameRules rules, Consumer<Void> onBack) {
        var gui = Gui.of(3)
                .title(Component.text("World Border Settings", NamedTextColor.DARK_AQUA))
                .statelessComponent(container -> {
                    // Toggle
                    container.setItem(10, makeToggle("World Border", rules.isWorldBorderEnabled(), Material.BARRIER, Material.STRUCTURE_VOID,
                            (p, ctx) -> { rules.setWorldBorderEnabled(!rules.isWorldBorderEnabled()); open(p, rules, onBack); }));

                    // Damage (+/-)
                    container.setItem(12, makeAdjustable("Damage/sec", String.format("%.1f", rules.getWorldBorderDamage()), Material.IRON_SWORD,
                            (p, ctx) -> {
                                if (ctx.guiClick() == GuiClick.RIGHT) rules.setWorldBorderDamage(rules.getWorldBorderDamage() - 0.5);
                                else rules.setWorldBorderDamage(rules.getWorldBorderDamage() + 0.5);
                                open(p, rules, onBack);
                            }));

                    // Close after (+/-)
                    container.setItem(14, makeAdjustable("Close After", rules.getWorldBorderCloseAfterSeconds() + "s", Material.CLOCK,
                            (p, ctx) -> {
                                GuiClick click = ctx.guiClick();
                                int delta = (click == GuiClick.SHIFT_LEFT || click == GuiClick.SHIFT_RIGHT) ? 60 : 10;
                                if (click == GuiClick.RIGHT || click == GuiClick.SHIFT_RIGHT) rules.setWorldBorderCloseAfterSeconds(rules.getWorldBorderCloseAfterSeconds() - delta);
                                else rules.setWorldBorderCloseAfterSeconds(rules.getWorldBorderCloseAfterSeconds() + delta);
                                open(p, rules, onBack);
                            }));

                    // Closing speed (+/-)
                    container.setItem(16, makeAdjustable("Close Speed", String.format("%.1f", rules.getWorldBorderCloseSpeed()) + " b/s", Material.FEATHER,
                            (p, ctx) -> {
                                if (ctx.guiClick() == GuiClick.RIGHT) rules.setWorldBorderCloseSpeed(rules.getWorldBorderCloseSpeed() - 0.5);
                                else rules.setWorldBorderCloseSpeed(rules.getWorldBorderCloseSpeed() + 0.5);
                                open(p, rules, onBack);
                            }));

                    // Back
                    container.setItem(22, ItemBuilder.from(Material.ARROW)
                            .name(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem((p, ctx) -> onBack.accept(null)));
                })
                .build();

        gui.open(player);
    }

    private GuiItem<Player, ItemStack> makeToggle(String name, boolean value, Material onMat, Material offMat,
                                                   dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        return ItemBuilder.from(value ? onMat : offMat)
                .name(Component.text(name, value ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text(value ? "ENABLED" : "DISABLED", value ? NamedTextColor.GREEN : NamedTextColor.RED)
                                .decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to toggle", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }

    private GuiItem<Player, ItemStack> makeAdjustable(String name, String value, Material icon,
                                                       dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        return ItemBuilder.from(icon)
                .name(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Current: " + value, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                        Component.empty(),
                        Component.text("Left-click: +", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                        Component.text("Right-click: -", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false),
                        Component.text("Shift: ×6 step", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }
}
