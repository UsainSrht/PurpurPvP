package com.usainsrht.purpurpvp.match;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Complete set of configurable game rules for a match.
 * Every field has a sensible default for competitive 1v1.
 */
public class GameRules {

    // ===== Blocks =====
    private boolean blockBreaking = false;
    private boolean blockPlacing = false;
    private boolean blockDrops = true;
    private boolean mapResetBetweenRounds = true;

    // ===== Explosions =====
    private boolean explosionsBreakBlocks = false;
    private boolean allowTNT = false;
    private boolean allowEndCrystal = false;
    private boolean allowRespawnAnchor = false;

    // ===== Items — toggle + cooldown =====
    private boolean shieldEnabled = true;
    private int shieldCooldownSeconds = 0;

    private boolean enderPearlEnabled = true;
    private int enderPearlCooldownSeconds = 0;

    private boolean tridentRiptideEnabled = true;
    private int tridentRiptideCooldownSeconds = 0;

    private boolean potionUsage = true;
    private boolean goldenApple = true;
    private boolean goldenHearts = true;       // absorption from gapple
    private boolean totemOfUndying = true;
    private boolean totemWorksInInventory = false;

    // ===== Arrows / Items =====
    private boolean pickUpArrows = false;
    private boolean dropItems = false;
    private boolean inventoryDropOnDeath = false;
    private boolean inventoryResetBetweenRounds = true;

    // ===== Crafting / Riding =====
    private boolean crafting = false;
    private boolean riding = false;

    // ===== Combat =====
    private boolean hunger = false;
    private boolean naturalRegen = false;
    private int roundsToWin = 1;

    // ===== World =====
    private boolean worldBorderEnabled = false;
    private double worldBorderDamage = 1.0;
    private int worldBorderCloseAfterSeconds = 300; // 5 min
    private double worldBorderCloseSpeed = 1.0;     // blocks/sec

    private DaytimeSetting daytime = DaytimeSetting.NOON;
    private WeatherSetting weather = WeatherSetting.CLEAR;

    // ===== Spectators =====
    private boolean spectatorsAllowed = true;

    // ===== Permanent Potion Effects (type -> amplifier 0-based) =====
    private final Map<PotionEffectType, Integer> permanentEffects = new HashMap<>();

    // ===== Getters / Setters =====

    public boolean isBlockBreaking() { return blockBreaking; }
    public void setBlockBreaking(boolean v) { blockBreaking = v; }

    public boolean isBlockPlacing() { return blockPlacing; }
    public void setBlockPlacing(boolean v) { blockPlacing = v; }

    public boolean isBlockDrops() { return blockDrops; }
    public void setBlockDrops(boolean v) { blockDrops = v; }

    public boolean isMapResetBetweenRounds() { return mapResetBetweenRounds; }
    public void setMapResetBetweenRounds(boolean v) { mapResetBetweenRounds = v; }

    public boolean isExplosionsBreakBlocks() { return explosionsBreakBlocks; }
    public void setExplosionsBreakBlocks(boolean v) { explosionsBreakBlocks = v; }

    public boolean isAllowTNT() { return allowTNT; }
    public void setAllowTNT(boolean v) { allowTNT = v; }

    public boolean isAllowEndCrystal() { return allowEndCrystal; }
    public void setAllowEndCrystal(boolean v) { allowEndCrystal = v; }

    public boolean isAllowRespawnAnchor() { return allowRespawnAnchor; }
    public void setAllowRespawnAnchor(boolean v) { allowRespawnAnchor = v; }

    public boolean isShieldEnabled() { return shieldEnabled; }
    public void setShieldEnabled(boolean v) { shieldEnabled = v; }
    public int getShieldCooldownSeconds() { return shieldCooldownSeconds; }
    public void setShieldCooldownSeconds(int v) { shieldCooldownSeconds = Math.max(0, v); }

    public boolean isEnderPearlEnabled() { return enderPearlEnabled; }
    public void setEnderPearlEnabled(boolean v) { enderPearlEnabled = v; }
    public int getEnderPearlCooldownSeconds() { return enderPearlCooldownSeconds; }
    public void setEnderPearlCooldownSeconds(int v) { enderPearlCooldownSeconds = Math.max(0, v); }

    public boolean isTridentRiptideEnabled() { return tridentRiptideEnabled; }
    public void setTridentRiptideEnabled(boolean v) { tridentRiptideEnabled = v; }
    public int getTridentRiptideCooldownSeconds() { return tridentRiptideCooldownSeconds; }
    public void setTridentRiptideCooldownSeconds(int v) { tridentRiptideCooldownSeconds = Math.max(0, v); }

    public boolean isPotionUsage() { return potionUsage; }
    public void setPotionUsage(boolean v) { potionUsage = v; }

    public boolean isGoldenApple() { return goldenApple; }
    public void setGoldenApple(boolean v) { goldenApple = v; }

    public boolean isGoldenHearts() { return goldenHearts; }
    public void setGoldenHearts(boolean v) { goldenHearts = v; }

    public boolean isTotemOfUndying() { return totemOfUndying; }
    public void setTotemOfUndying(boolean v) { totemOfUndying = v; }

    public boolean isTotemWorksInInventory() { return totemWorksInInventory; }
    public void setTotemWorksInInventory(boolean v) { totemWorksInInventory = v; }

    public boolean isPickUpArrows() { return pickUpArrows; }
    public void setPickUpArrows(boolean v) { pickUpArrows = v; }

    public boolean isDropItems() { return dropItems; }
    public void setDropItems(boolean v) { dropItems = v; }

    public boolean isInventoryDropOnDeath() { return inventoryDropOnDeath; }
    public void setInventoryDropOnDeath(boolean v) { inventoryDropOnDeath = v; }

    public boolean isInventoryResetBetweenRounds() { return inventoryResetBetweenRounds; }
    public void setInventoryResetBetweenRounds(boolean v) { inventoryResetBetweenRounds = v; }

    public boolean isCrafting() { return crafting; }
    public void setCrafting(boolean v) { crafting = v; }

    public boolean isRiding() { return riding; }
    public void setRiding(boolean v) { riding = v; }

    public boolean isHunger() { return hunger; }
    public void setHunger(boolean v) { hunger = v; }

    public boolean isNaturalRegen() { return naturalRegen; }
    public void setNaturalRegen(boolean v) { naturalRegen = v; }

    public int getRoundsToWin() { return roundsToWin; }
    public void setRoundsToWin(int v) { roundsToWin = Math.max(1, v); }

    public boolean isWorldBorderEnabled() { return worldBorderEnabled; }
    public void setWorldBorderEnabled(boolean v) { worldBorderEnabled = v; }

    public double getWorldBorderDamage() { return worldBorderDamage; }
    public void setWorldBorderDamage(double v) { worldBorderDamage = Math.max(0, v); }

    public int getWorldBorderCloseAfterSeconds() { return worldBorderCloseAfterSeconds; }
    public void setWorldBorderCloseAfterSeconds(int v) { worldBorderCloseAfterSeconds = Math.max(0, v); }

    public double getWorldBorderCloseSpeed() { return worldBorderCloseSpeed; }
    public void setWorldBorderCloseSpeed(double v) { worldBorderCloseSpeed = Math.max(0.1, v); }

    public DaytimeSetting getDaytime() { return daytime; }
    public void setDaytime(DaytimeSetting v) { daytime = v; }

    public WeatherSetting getWeather() { return weather; }
    public void setWeather(WeatherSetting v) { weather = v; }

    public boolean isSpectatorsAllowed() { return spectatorsAllowed; }
    public void setSpectatorsAllowed(boolean v) { spectatorsAllowed = v; }

    public Map<PotionEffectType, Integer> getPermanentEffects() { return permanentEffects; }
}


