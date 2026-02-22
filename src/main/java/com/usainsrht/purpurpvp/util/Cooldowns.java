package com.usainsrht.purpurpvp.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic cooldown tracker for any keyed action.
 */
public final class Cooldowns {

    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Set a cooldown for a player + action key.
     */
    public void setCooldown(UUID player, String action, long durationMillis) {
        cooldowns.put(player + ":" + action, System.currentTimeMillis() + durationMillis);
    }

    /**
     * Check if a cooldown is active.
     */
    public boolean isOnCooldown(UUID player, String action) {
        Long expiry = cooldowns.get(player + ":" + action);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) {
            cooldowns.remove(player + ":" + action);
            return false;
        }
        return true;
    }

    /**
     * Get remaining cooldown time in milliseconds.
     */
    public long getRemainingMillis(UUID player, String action) {
        Long expiry = cooldowns.get(player + ":" + action);
        if (expiry == null) return 0;
        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    public void clearAll() {
        cooldowns.clear();
    }
}

