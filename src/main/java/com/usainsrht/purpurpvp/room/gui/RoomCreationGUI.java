package com.usainsrht.purpurpvp.room.gui;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.match.GameRules;
import com.usainsrht.purpurpvp.match.MatchConfig;
import com.usainsrht.purpurpvp.room.Room;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Multi-page Room Configuration GUI.
 * Page 1: Core settings + Combat/Item toggles
 * Page 2: World/Environment + Sub-GUIs (World Border, Permanent Effects, Map)
 */
public class RoomCreationGUI {

    private final PurpurPvP plugin;
    private final MapSelectionGUI mapSelectionGUI;
    private final PermanentEffectsGUI permanentEffectsGUI;
    private final WorldBorderSettingsGUI worldBorderSettingsGUI;

    public RoomCreationGUI(PurpurPvP plugin) {
        this.plugin = plugin;
        this.mapSelectionGUI = new MapSelectionGUI(plugin);
        this.permanentEffectsGUI = new PermanentEffectsGUI();
        this.worldBorderSettingsGUI = new WorldBorderSettingsGUI();
    }

    public void open(Player player, Room room) {
        openPage1(player, room);
    }

    // =========================================================================
    // PAGE 1
    // =========================================================================
    private void openPage1(Player player, Room room) {
        MatchConfig cfg = room.getMatchConfig();
        GameRules r = cfg.getGameRules();

        var gui = Gui.of(6)
                .title(Component.text("Room Settings (1/2)", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    // Row 1: Core
                    container.setItem(1, makeInfo(Material.IRON_SWORD, "Kit",
                            cfg.getKit() != null ? cfg.getKit().getName() : "None", NamedTextColor.GOLD,
                            (p, ctx) -> plugin.getKitSelectorGUI().open(p, kit -> { cfg.setKit(kit); openPage1(p, room); })));
                    container.setItem(3, makeInfo(Material.FILLED_MAP, "Map",
                            cfg.getArena() != null ? cfg.getArena().getName() : "Random", NamedTextColor.GREEN,
                            (p, ctx) -> mapSelectionGUI.open(p, room, v -> openPage1(p, room))));
                    container.setItem(5, makeInfo(Material.PLAYER_HEAD, "Team Size",
                            cfg.getTeamSize() + "v" + cfg.getTeamSize(), NamedTextColor.AQUA,
                            (p, ctx) -> { cfg.setTeamSize(cfg.getTeamSize() % 4 + 1); openPage1(p, room); }));
                    container.setItem(6, makeInfo(Material.PAPER, "Teams",
                            cfg.getTeamCount() + " teams", NamedTextColor.YELLOW,
                            (p, ctx) -> { cfg.setTeamCount(cfg.getTeamCount() == 2 ? 4 : 2); openPage1(p, room); }));
                    container.setItem(7, makeAdjustable(Material.GOLD_INGOT, "Rounds to Win", String.valueOf(r.getRoundsToWin()), (p, ctx) -> {
                        if (ctx.guiClick() == GuiClick.RIGHT) r.setRoundsToWin(r.getRoundsToWin() - 1);
                        else r.setRoundsToWin(r.getRoundsToWin() + 1);
                        openPage1(p, room);
                    }));
                    container.setItem(8, makeToggle(Material.DIAMOND, "Ranked", cfg.isRanked(),
                            (p, ctx) -> { cfg.setRanked(!cfg.isRanked()); openPage1(p, room); }));

                    // Row 2: separator
                    for (int i = 9; i < 18; i++) container.setItem(i, pane());

                    // Row 3: Items/Combat
                    container.setItem(18, makeToggle(Material.SHIELD, "Shields", r.isShieldEnabled(), (p, ctx) -> { r.setShieldEnabled(!r.isShieldEnabled()); openPage1(p, room); }));
                    container.setItem(19, makeAdjustable(Material.SHIELD, "Shield CD", r.getShieldCooldownSeconds() + "s", (p, ctx) -> {
                        if (ctx.guiClick() == GuiClick.RIGHT) r.setShieldCooldownSeconds(r.getShieldCooldownSeconds() - 1);
                        else r.setShieldCooldownSeconds(r.getShieldCooldownSeconds() + 1);
                        openPage1(p, room);
                    }));
                    container.setItem(20, makeToggle(Material.ENDER_PEARL, "Ender Pearls", r.isEnderPearlEnabled(), (p, ctx) -> { r.setEnderPearlEnabled(!r.isEnderPearlEnabled()); openPage1(p, room); }));
                    container.setItem(21, makeAdjustable(Material.ENDER_PEARL, "Pearl CD", r.getEnderPearlCooldownSeconds() + "s", (p, ctx) -> {
                        if (ctx.guiClick() == GuiClick.RIGHT) r.setEnderPearlCooldownSeconds(r.getEnderPearlCooldownSeconds() - 1);
                        else r.setEnderPearlCooldownSeconds(r.getEnderPearlCooldownSeconds() + 1);
                        openPage1(p, room);
                    }));
                    container.setItem(22, makeToggle(Material.TRIDENT, "Trident/Riptide", r.isTridentRiptideEnabled(), (p, ctx) -> { r.setTridentRiptideEnabled(!r.isTridentRiptideEnabled()); openPage1(p, room); }));
                    container.setItem(23, makeAdjustable(Material.TRIDENT, "Trident CD", r.getTridentRiptideCooldownSeconds() + "s", (p, ctx) -> {
                        if (ctx.guiClick() == GuiClick.RIGHT) r.setTridentRiptideCooldownSeconds(r.getTridentRiptideCooldownSeconds() - 1);
                        else r.setTridentRiptideCooldownSeconds(r.getTridentRiptideCooldownSeconds() + 1);
                        openPage1(p, room);
                    }));
                    container.setItem(24, makeToggle(Material.SPLASH_POTION, "Potions", r.isPotionUsage(), (p, ctx) -> { r.setPotionUsage(!r.isPotionUsage()); openPage1(p, room); }));
                    container.setItem(25, makeToggle(Material.GOLDEN_APPLE, "Golden Apple", r.isGoldenApple(), (p, ctx) -> { r.setGoldenApple(!r.isGoldenApple()); openPage1(p, room); }));
                    container.setItem(26, makeToggle(Material.ENCHANTED_GOLDEN_APPLE, "Golden Hearts", r.isGoldenHearts(), (p, ctx) -> { r.setGoldenHearts(!r.isGoldenHearts()); openPage1(p, room); }));

                    // Row 4: More toggles
                    container.setItem(27, makeToggle(Material.TOTEM_OF_UNDYING, "Totem", r.isTotemOfUndying(), (p, ctx) -> { r.setTotemOfUndying(!r.isTotemOfUndying()); openPage1(p, room); }));
                    container.setItem(28, makeToggle(Material.CHEST, "Totem in Inv", r.isTotemWorksInInventory(), (p, ctx) -> { r.setTotemWorksInInventory(!r.isTotemWorksInInventory()); openPage1(p, room); }));
                    container.setItem(29, makeToggle(Material.COOKED_BEEF, "Hunger", r.isHunger(), (p, ctx) -> { r.setHunger(!r.isHunger()); openPage1(p, room); }));
                    container.setItem(30, makeToggle(Material.APPLE, "Natural Regen", r.isNaturalRegen(), (p, ctx) -> { r.setNaturalRegen(!r.isNaturalRegen()); openPage1(p, room); }));
                    container.setItem(31, makeToggle(Material.CRAFTING_TABLE, "Crafting", r.isCrafting(), (p, ctx) -> { r.setCrafting(!r.isCrafting()); openPage1(p, room); }));
                    container.setItem(32, makeToggle(Material.SADDLE, "Riding", r.isRiding(), (p, ctx) -> { r.setRiding(!r.isRiding()); openPage1(p, room); }));
                    container.setItem(33, makeToggle(Material.SPECTRAL_ARROW, "Arrow Pickup", r.isPickUpArrows(), (p, ctx) -> { r.setPickUpArrows(!r.isPickUpArrows()); openPage1(p, room); }));
                    container.setItem(34, makeToggle(Material.DROPPER, "Drop Items", r.isDropItems(), (p, ctx) -> { r.setDropItems(!r.isDropItems()); openPage1(p, room); }));
                    container.setItem(35, makeToggle(Material.SPYGLASS, "Spectators", r.isSpectatorsAllowed(), (p, ctx) -> { r.setSpectatorsAllowed(!r.isSpectatorsAllowed()); openPage1(p, room); }));

                    // Row 5: Block / Inventory
                    container.setItem(36, makeToggle(Material.DIAMOND_PICKAXE, "Block Break", r.isBlockBreaking(), (p, ctx) -> { r.setBlockBreaking(!r.isBlockBreaking()); openPage1(p, room); }));
                    container.setItem(37, makeToggle(Material.BRICKS, "Block Place", r.isBlockPlacing(), (p, ctx) -> { r.setBlockPlacing(!r.isBlockPlacing()); openPage1(p, room); }));
                    container.setItem(38, makeToggle(Material.FLINT_AND_STEEL, "Block Drops", r.isBlockDrops(), (p, ctx) -> { r.setBlockDrops(!r.isBlockDrops()); openPage1(p, room); }));
                    container.setItem(39, makeToggle(Material.SKELETON_SKULL, "Death Inv Drop", r.isInventoryDropOnDeath(), (p, ctx) -> { r.setInventoryDropOnDeath(!r.isInventoryDropOnDeath()); openPage1(p, room); }));
                    container.setItem(40, makeToggle(Material.ENDER_CHEST, "Inv Reset/Round", r.isInventoryResetBetweenRounds(), (p, ctx) -> { r.setInventoryResetBetweenRounds(!r.isInventoryResetBetweenRounds()); openPage1(p, room); }));
                    container.setItem(41, makeToggle(Material.GRASS_BLOCK, "Map Reset/Round", r.isMapResetBetweenRounds(), (p, ctx) -> { r.setMapResetBetweenRounds(!r.isMapResetBetweenRounds()); openPage1(p, room); }));

                    // Row 6: Nav
                    container.setItem(45, closeButton(room));
                    container.setItem(49, startButton(room));
                    container.setItem(53, makeNav(Material.ARROW, "Page 2 →", NamedTextColor.AQUA, (p, ctx) -> openPage2(p, room)));
                })
                .build();

        gui.open(player);
    }

    // =========================================================================
    // PAGE 2
    // =========================================================================
    private void openPage2(Player player, Room room) {
        GameRules r = room.getMatchConfig().getGameRules();

        var gui = Gui.of(6)
                .title(Component.text("Room Settings (2/2)", NamedTextColor.DARK_PURPLE))
                .statelessComponent(container -> {
                    // Row 1: Explosions
                    container.setItem(0, makeToggle(Material.TNT, "TNT", r.isAllowTNT(), (p, ctx) -> { r.setAllowTNT(!r.isAllowTNT()); openPage2(p, room); }));
                    container.setItem(1, makeToggle(Material.END_CRYSTAL, "End Crystal", r.isAllowEndCrystal(), (p, ctx) -> { r.setAllowEndCrystal(!r.isAllowEndCrystal()); openPage2(p, room); }));
                    container.setItem(2, makeToggle(Material.RESPAWN_ANCHOR, "Respawn Anchor", r.isAllowRespawnAnchor(), (p, ctx) -> { r.setAllowRespawnAnchor(!r.isAllowRespawnAnchor()); openPage2(p, room); }));
                    container.setItem(3, makeToggle(Material.FIRE_CHARGE, "Explosions Break", r.isExplosionsBreakBlocks(), (p, ctx) -> { r.setExplosionsBreakBlocks(!r.isExplosionsBreakBlocks()); openPage2(p, room); }));

                    // Row 2: Daytime & Weather
                    container.setItem(9, makeInfo(Material.SUNFLOWER, "Daytime", r.getDaytime().name(), NamedTextColor.YELLOW,
                            (p, ctx) -> { r.setDaytime(r.getDaytime().next()); openPage2(p, room); }));
                    container.setItem(10, makeInfo(Material.WATER_BUCKET, "Weather", r.getWeather().name(), NamedTextColor.AQUA,
                            (p, ctx) -> { r.setWeather(r.getWeather().next()); openPage2(p, room); }));

                    // Row 3: Sub-GUI buttons
                    // World Border
                    List<Component> wbLore = new ArrayList<>();
                    wbLore.add(Component.text(r.isWorldBorderEnabled() ? "ENABLED" : "DISABLED",
                            r.isWorldBorderEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
                    wbLore.add(Component.text("Damage: " + String.format("%.1f", r.getWorldBorderDamage()), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    wbLore.add(Component.text("Close after: " + r.getWorldBorderCloseAfterSeconds() + "s", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    wbLore.add(Component.text("Speed: " + String.format("%.1f", r.getWorldBorderCloseSpeed()) + " b/s", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    wbLore.add(Component.empty());
                    wbLore.add(Component.text("Click to configure", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                    container.setItem(18, ItemBuilder.from(Material.IRON_BARS)
                            .name(Component.text("World Border Settings", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false))
                            .lore(wbLore)
                            .asGuiItem((p, ctx) -> worldBorderSettingsGUI.open(p, r, v -> openPage2(p, room))));

                    // Permanent Effects
                    container.setItem(20, ItemBuilder.from(Material.BREWING_STAND)
                            .name(Component.text("Permanent Effects", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false))
                            .lore(
                                    Component.text("Active: " + r.getPermanentEffects().size() + " effect(s)", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false),
                                    Component.text("Click to configure", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                            )
                            .asGuiItem((p, ctx) -> permanentEffectsGUI.open(p, r, v -> openPage2(p, room))));

                    // Row 6: Nav
                    container.setItem(45, makeNav(Material.ARROW, "← Page 1", NamedTextColor.AQUA, (p, ctx) -> openPage1(p, room)));
                    container.setItem(49, startButton(room));
                    container.setItem(53, closeButton(room));
                })
                .build();

        gui.open(player);
    }

    // =========================================================================
    // Item builders — all return GuiItem with action baked in
    // =========================================================================

    private GuiItem<Player, ItemStack> makeToggle(Material mat, String name, boolean enabled,
                                                   dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        NamedTextColor color = enabled ? NamedTextColor.GREEN : NamedTextColor.RED;
        return ItemBuilder.from(mat)
                .name(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text(enabled ? "✔ ENABLED" : "✘ DISABLED", color).decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to toggle", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }

    private GuiItem<Player, ItemStack> makeAdjustable(Material mat, String name, String value,
                                                       dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        return ItemBuilder.from(mat)
                .name(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text("Value: " + value, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                        Component.text("Left: + | Right: -", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }

    private GuiItem<Player, ItemStack> makeInfo(Material mat, String name, String value, NamedTextColor color,
                                                 dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        return ItemBuilder.from(mat)
                .name(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
                .lore(
                        Component.text(value, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
                        Component.text("Click to change", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )
                .asGuiItem(action);
    }

    private GuiItem<Player, ItemStack> makeNav(Material mat, String text, NamedTextColor color,
                                                dev.triumphteam.gui.click.action.SimpleGuiClickAction<Player> action) {
        return ItemBuilder.from(mat)
                .name(Component.text(text, color).decoration(TextDecoration.ITALIC, false))
                .asGuiItem(action);
    }

    private GuiItem<Player, ItemStack> pane() {
        return ItemBuilder.from(Material.PURPLE_STAINED_GLASS_PANE)
                .name(Component.text(" "))
                .asGuiItem();
    }

    private GuiItem<Player, ItemStack> startButton(Room room) {
        return ItemBuilder.from(Material.EMERALD_BLOCK)
                .name(Component.text("START MATCH", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false))
                .lore(Component.text("Players: " + room.getPlayerCount() + "/" + room.getMaxPlayers(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .asGuiItem((p, ctx) -> {
                    p.closeInventory();
                    plugin.getRoomManager().startRoom(room.getRoomId());
                });
    }

    private GuiItem<Player, ItemStack> closeButton(Room room) {
        return ItemBuilder.from(Material.BARRIER)
                .name(Component.text("Close Room", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false))
                .asGuiItem((p, ctx) -> {
                    p.closeInventory();
                    plugin.getRoomManager().closeRoom(room.getRoomId());
                });
    }
}
