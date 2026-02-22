package com.usainsrht.purpurpvp.room.gui;

import com.usainsrht.purpurpvp.match.GameRules;
import dev.triumphteam.gui.click.GuiClick;
import dev.triumphteam.gui.paper.Gui;
import dev.triumphteam.gui.paper.builder.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.potion.PotionEffectType;

/**
 * GUI for selecting permanent potion effects and their amplifier (tier).
 * These effects persist through death and milk during the match.
 */
public class PermanentEffectsGUI {

    // Ordered list of potion effects with display info
    private static final List<EffectEntry> EFFECTS = List.of(
            new EffectEntry(PotionEffectType.SPEED, Material.SUGAR, "Speed"),
            new EffectEntry(PotionEffectType.SLOWNESS, Material.COBWEB, "Slowness"),
            new EffectEntry(PotionEffectType.HASTE, Material.GOLDEN_PICKAXE, "Haste"),
            new EffectEntry(PotionEffectType.MINING_FATIGUE, Material.CLAY_BALL, "Mining Fatigue"),
            new EffectEntry(PotionEffectType.STRENGTH, Material.BLAZE_POWDER, "Strength"),
            new EffectEntry(PotionEffectType.INSTANT_HEALTH, Material.GLISTERING_MELON_SLICE, "Instant Health"),
            new EffectEntry(PotionEffectType.INSTANT_DAMAGE, Material.FERMENTED_SPIDER_EYE, "Instant Damage"),
            new EffectEntry(PotionEffectType.JUMP_BOOST, Material.RABBIT_FOOT, "Jump Boost"),
            new EffectEntry(PotionEffectType.NAUSEA, Material.POISONOUS_POTATO, "Nausea"),
            new EffectEntry(PotionEffectType.REGENERATION, Material.GHAST_TEAR, "Regeneration"),
            new EffectEntry(PotionEffectType.RESISTANCE, Material.IRON_CHESTPLATE, "Resistance"),
            new EffectEntry(PotionEffectType.FIRE_RESISTANCE, Material.MAGMA_CREAM, "Fire Resistance"),
            new EffectEntry(PotionEffectType.WATER_BREATHING, Material.PUFFERFISH, "Water Breathing"),
            new EffectEntry(PotionEffectType.INVISIBILITY, Material.GLASS, "Invisibility"),
            new EffectEntry(PotionEffectType.BLINDNESS, Material.INK_SAC, "Blindness"),
            new EffectEntry(PotionEffectType.NIGHT_VISION, Material.GOLDEN_CARROT, "Night Vision"),
            new EffectEntry(PotionEffectType.HUNGER, Material.ROTTEN_FLESH, "Hunger"),
            new EffectEntry(PotionEffectType.WEAKNESS, Material.WOODEN_SWORD, "Weakness"),
            new EffectEntry(PotionEffectType.POISON, Material.SPIDER_EYE, "Poison"),
            new EffectEntry(PotionEffectType.WITHER, Material.WITHER_SKELETON_SKULL, "Wither"),
            new EffectEntry(PotionEffectType.HEALTH_BOOST, Material.APPLE, "Health Boost"),
            new EffectEntry(PotionEffectType.ABSORPTION, Material.GOLDEN_APPLE, "Absorption"),
            new EffectEntry(PotionEffectType.SATURATION, Material.COOKED_BEEF, "Saturation"),
            new EffectEntry(PotionEffectType.GLOWING, Material.SPECTRAL_ARROW, "Glowing"),
            new EffectEntry(PotionEffectType.LEVITATION, Material.SHULKER_SHELL, "Levitation"),
            new EffectEntry(PotionEffectType.SLOW_FALLING, Material.PHANTOM_MEMBRANE, "Slow Falling"),
            new EffectEntry(PotionEffectType.DOLPHINS_GRACE, Material.HEART_OF_THE_SEA, "Dolphin's Grace"),
            new EffectEntry(PotionEffectType.DARKNESS, Material.SCULK, "Darkness")
    );

    public void open(Player player, GameRules rules, Consumer<Void> onBack) {
        Map<PotionEffectType, Integer> effects = rules.getPermanentEffects();

        var gui = Gui.of(6)
                .title(Component.text("Permanent Effects", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    int slot = 0;
                    for (EffectEntry entry : EFFECTS) {
                        if (slot >= 45) break;

                        Integer currentAmp = effects.get(entry.type);
                        boolean enabled = currentAmp != null;
                        int tier = enabled ? currentAmp + 1 : 0; // display tier (1-based)

                        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY;

                        List<Component> lore = new ArrayList<>();
                        if (enabled) {
                            lore.add(Component.text("Tier: " + toRoman(tier), NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
                        } else {
                            lore.add(Component.text("Disabled", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                        }
                        lore.add(Component.empty());
                        lore.add(Component.text("Left-click: Toggle", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("Right-click: + Tier", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                        lore.add(Component.text("Shift-click: - Tier", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

                        var itemBuilder = ItemBuilder.from(entry.icon)
                                .name(Component.text(entry.name, color).decoration(TextDecoration.ITALIC, false))
                                .lore(lore);
                        if (enabled) itemBuilder.amount(Math.min(tier, 64));

                        container.setItem(slot++, itemBuilder.asGuiItem((p, ctx) -> {
                            GuiClick click = ctx.guiClick();
                            if (click == GuiClick.SHIFT_LEFT || click == GuiClick.SHIFT_RIGHT) {
                                // Decrease tier
                                Integer amp = effects.get(entry.type);
                                if (amp != null && amp > 0) {
                                    effects.put(entry.type, amp - 1);
                                } else {
                                    effects.remove(entry.type);
                                }
                            } else if (click == GuiClick.RIGHT) {
                                // Increase tier
                                Integer amp = effects.get(entry.type);
                                if (amp == null) {
                                    effects.put(entry.type, 0);
                                } else if (amp < 9) {
                                    effects.put(entry.type, amp + 1);
                                }
                            } else {
                                // Toggle
                                if (effects.containsKey(entry.type)) {
                                    effects.remove(entry.type);
                                } else {
                                    effects.put(entry.type, 0); // Tier I
                                }
                            }
                            open(p, rules, onBack);
                        }));
                    }

                    // Clear all
                    container.setItem(48, ItemBuilder.from(Material.TNT)
                            .name(Component.text("Clear All Effects", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem((p, ctx) -> {
                                effects.clear();
                                open(p, rules, onBack);
                            }));

                    // Active count
                    container.setItem(49, ItemBuilder.from(Material.BOOK)
                            .name(Component.text("Active: " + effects.size() + " effects", NamedTextColor.AQUA)
                                    .decoration(TextDecoration.ITALIC, false))
                            .asGuiItem());

                    // Back
                    container.setItem(50, ItemBuilder.from(Material.ARROW)
                            .name(Component.text("Back", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                            .asGuiItem((p, ctx) -> onBack.accept(null)));
                })
                .build();

        gui.open(player);
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private record EffectEntry(PotionEffectType type, Material icon, String name) {}
}
