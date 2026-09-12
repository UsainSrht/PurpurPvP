package com.usainsrht.purpurpvp.match;

import com.usainsrht.purpurpvp.PurpurPvP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Enforces all {@link GameRules} during active matches.
 */
public class MatchListener implements Listener {

    private final PurpurPvP plugin;
    private final MatchManager matchManager;

    public MatchListener(PurpurPvP plugin) {
        this.plugin = plugin;
        this.matchManager = plugin.getMatchManager();
    }

    // ========== Death / Disconnect ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Match match = matchManager.getMatchOfPlayer(dead.getUniqueId());
        if (match == null) return;
        GameRules rules = match.rules();

        event.deathMessage(null);
        event.setDroppedExp(0);
        event.setKeepLevel(false);

        if (rules.isInventoryDropOnDeath()) {
            event.setKeepInventory(false);
        } else {
            event.getDrops().clear();
            event.setKeepInventory(false);
        }

        UUID killerUuid = dead.getKiller() != null ? dead.getKiller().getUniqueId() : null;
        match.onPlayerDeath(dead.getUniqueId(), killerUuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Match match = matchManager.getMatchOfPlayer(event.getPlayer().getUniqueId());
        if (match == null) return;
        match.onPlayerDisconnect(event.getPlayer().getUniqueId());
    }

    // ========== Blocks ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Match match = matchManager.getMatchOfPlayer(event.getPlayer().getUniqueId());
        if (match == null) return;
        if (!match.rules().isBlockBreaking()) {
            event.setCancelled(true);
            return;
        }
        // Track for map reset
        match.trackBlockChange(event.getBlock().getLocation(), event.getBlock().getType());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Match match = matchManager.getMatchOfPlayer(event.getPlayer().getUniqueId());
        if (match == null) return;
        if (!match.rules().isBlockPlacing()) {
            event.setCancelled(true);
            return;
        }
        // Track for map reset (original was air)
        match.trackBlockChange(event.getBlock().getLocation(), event.getBlockReplacedState().getType());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match != null && !match.rules().isBlockDrops()) {
            event.setCancelled(true);
        }
    }

    // ========== Explosions ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Check if any match participant is near
        for (Match match : matchManager.getActiveMatches()) {
            if (match.getState() != MatchState.ACTIVE) continue;
            GameRules rules = match.rules();

            // Check entity type
            Entity entity = event.getEntity();
            if (entity instanceof TNTPrimed && !rules.isAllowTNT()) {
                event.setCancelled(true);
                return;
            }
            if (entity instanceof EnderCrystal && !rules.isAllowEndCrystal()) {
                event.setCancelled(true);
                return;
            }

            // Block destruction from explosions
            if (!rules.isExplosionsBreakBlocks()) {
                event.blockList().clear();
            } else {
                // Track changes for map reset
                for (var block : event.blockList()) {
                    match.trackBlockChange(block.getLocation(), block.getType());
                }
            }
        }
    }

    // ========== Combat Damage / Friendly Fire ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player damager = null;
        if (event.getDamager() instanceof Player p) {
            damager = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }
        if (damager == null) return;

        Match match = matchManager.getMatchOfPlayer(damager.getUniqueId());
        if (match == null) return;

        if (match.getState() != MatchState.ACTIVE) {
            event.setCancelled(true);
            return;
        }

        // Friendly fire check
        if (match.getTeamOf(damager.getUniqueId()) == match.getTeamOf(victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    // ========== Shield / Ender Pearl / Trident with Cooldowns ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;
        GameRules rules = match.rules();
        ItemStack item = event.getItem();
        if (item == null) return;

        Material type = item.getType();

        // Shield
        if (type == Material.SHIELD) {
            if (!rules.isShieldEnabled()) {
                event.setCancelled(true);
                return;
            }
            if (rules.getShieldCooldownSeconds() > 0) {
                if (match.getCooldowns().isOnCooldown(player.getUniqueId(), "shield")) {
                    event.setCancelled(true);
                    long remaining = match.getCooldowns().getRemainingMillis(player.getUniqueId(), "shield");
                    plugin.getMessageService().send(player, "match.shield-cooldown", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("seconds", String.valueOf(remaining / 1000 + 1)));
                    return;
                }
                match.getCooldowns().setCooldown(player.getUniqueId(), "shield", rules.getShieldCooldownSeconds() * 1000L);
            }
        }

        // Ender pearl
        if (type == Material.ENDER_PEARL) {
            if (!rules.isEnderPearlEnabled()) {
                event.setCancelled(true);
                return;
            }
            if (rules.getEnderPearlCooldownSeconds() > 0) {
                if (match.getCooldowns().isOnCooldown(player.getUniqueId(), "enderpearl")) {
                    event.setCancelled(true);
                    long remaining = match.getCooldowns().getRemainingMillis(player.getUniqueId(), "enderpearl");
                    plugin.getMessageService().send(player, "match.pearl-cooldown", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("seconds", String.valueOf(remaining / 1000 + 1)));
                    return;
                }
                match.getCooldowns().setCooldown(player.getUniqueId(), "enderpearl", rules.getEnderPearlCooldownSeconds() * 1000L);
            }
        }

        // Trident (riptide)
        if (type == Material.TRIDENT) {
            if (!rules.isTridentRiptideEnabled()) {
                event.setCancelled(true);
                return;
            }
            if (rules.getTridentRiptideCooldownSeconds() > 0) {
                if (match.getCooldowns().isOnCooldown(player.getUniqueId(), "trident")) {
                    event.setCancelled(true);
                    long remaining = match.getCooldowns().getRemainingMillis(player.getUniqueId(), "trident");
                    plugin.getMessageService().send(player, "match.trident-cooldown", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("seconds", String.valueOf(remaining / 1000 + 1)));
                    return;
                }
                match.getCooldowns().setCooldown(player.getUniqueId(), "trident", rules.getTridentRiptideCooldownSeconds() * 1000L);
            }
        }

        // Potions
        if ((type == Material.SPLASH_POTION || type == Material.LINGERING_POTION || type == Material.POTION)
                && !rules.isPotionUsage()) {
            event.setCancelled(true);
        }

        // Golden apple
        if ((type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE)
                && !rules.isGoldenApple()) {
            event.setCancelled(true);
        }

        // Respawn anchor
        if (type == Material.RESPAWN_ANCHOR && !rules.isAllowRespawnAnchor()) {
            event.setCancelled(true);
        }
    }

    // ========== Consume: Golden apple absorption removal ==========

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;
        GameRules rules = match.rules();

        Material type = event.getItem().getType();

        // Block golden apple consumption
        if ((type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE) && !rules.isGoldenApple()) {
            event.setCancelled(true);
            return;
        }

        // Remove golden hearts (absorption) after eating gapple
        if (!rules.isGoldenHearts() && (type == Material.GOLDEN_APPLE || type == Material.ENCHANTED_GOLDEN_APPLE)) {
            // Schedule removal on next tick (after the effect is applied)
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> player.removePotionEffect(PotionEffectType.ABSORPTION), 1L);
        }
    }

    // ========== Totem of Undying ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;

        if (!match.rules().isTotemOfUndying()) {
            event.setCancelled(true);
            return;
        }

        // If totem works in inventory, and the player doesn't have one in hand,
        // check if they have one anywhere in inventory and consume it to allow resurrection.
        if (match.rules().isTotemWorksInInventory() && event.isCancelled()) {
            // The event was cancelled because vanilla didn't find a totem in hand.
            // Search inventory for a totem.
            int totemSlot = player.getInventory().first(Material.TOTEM_OF_UNDYING);
            if (totemSlot != -1) {
                ItemStack totem = player.getInventory().getItem(totemSlot);
                if (totem != null) {
                    totem.setAmount(totem.getAmount() - 1);
                    event.setCancelled(false); // allow resurrection
                }
            }
        }
    }

    // ========== Food / Hunger ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match != null && !match.rules().isHunger()) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20f);
        }
    }

    // ========== Health Regen ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onHealthRegen(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;

        if (!match.rules().isNaturalRegen()
                && event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            event.setCancelled(true);
        }
    }

    // ========== Arrow Pickup ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupArrow(PlayerPickupArrowEvent event) {
        Match match = matchManager.getMatchOfPlayer(event.getPlayer().getUniqueId());
        if (match != null && !match.rules().isPickUpArrows()) {
            event.setCancelled(true);
        }
    }

    // ========== Item Drop ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Match match = matchManager.getMatchOfPlayer(event.getPlayer().getUniqueId());
        if (match != null && !match.rules().isDropItems()) {
            event.setCancelled(true);
        }
    }

    // ========== Crafting ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match != null && !match.rules().isCrafting()) {
            event.setCancelled(true);
        }
    }

    // ========== Riding ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match != null && !match.rules().isRiding()) {
            event.setCancelled(true);
        }
    }

    // ========== Milk (remove permanent effects re-application) ==========

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMilkConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;
        Player player = event.getPlayer();
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;

        // Re-apply permanent effects after milk clears them (next tick)
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> match.applyPermanentEffects(player), 1L);
    }

    // ========== Respawn (re-apply permanent effects) ==========

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Match match = matchManager.getMatchOfPlayer(player.getUniqueId());
        if (match == null) return;

        // Re-apply permanent effects after respawn
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> match.applyPermanentEffects(player), 1L);
    }
}
