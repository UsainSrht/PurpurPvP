package com.usainsrht.purpurpvp.ranked;

import java.util.UUID;

/**
 * In-memory player profile with XP, level, rating, and stats.
 */
public class PlayerProfile {

    private final UUID uuid;
    private String username;

    // Progression
    private long xp;
    private int level;

    // Rating
    private double eloRating;
    private double glickoRating;
    private double glickoRd;
    private double glickoVolatility;

    // Stats
    private int wins;
    private int losses;
    private int kills;
    private int deaths;

    private long lastSeen;
    private boolean dirty; // true if needs saving

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.xp = 0;
        this.level = 1;
        this.eloRating = 1000.0;
        this.glickoRating = 1500.0;
        this.glickoRd = 350.0;
        this.glickoVolatility = 0.06;
        this.wins = 0;
        this.losses = 0;
        this.kills = 0;
        this.deaths = 0;
        this.lastSeen = System.currentTimeMillis();
        this.dirty = false;
    }

    // ===== Getters/Setters =====

    public UUID getUuid() { return uuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; this.dirty = true; }

    public long getXp() { return xp; }
    public void setXp(long xp) { this.xp = xp; this.dirty = true; }
    public void addXp(long amount) { this.xp += amount; this.dirty = true; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; this.dirty = true; }

    public double getEloRating() { return eloRating; }
    public void setEloRating(double eloRating) { this.eloRating = eloRating; this.dirty = true; }

    public double getGlickoRating() { return glickoRating; }
    public void setGlickoRating(double glickoRating) { this.glickoRating = glickoRating; this.dirty = true; }

    public double getGlickoRd() { return glickoRd; }
    public void setGlickoRd(double glickoRd) { this.glickoRd = glickoRd; this.dirty = true; }

    public double getGlickoVolatility() { return glickoVolatility; }
    public void setGlickoVolatility(double glickoVolatility) { this.glickoVolatility = glickoVolatility; this.dirty = true; }

    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; this.dirty = true; }
    public void incrementWins() { this.wins++; this.dirty = true; }

    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; this.dirty = true; }
    public void incrementLosses() { this.losses++; this.dirty = true; }

    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; this.dirty = true; }
    public void addKills(int amount) { this.kills += amount; this.dirty = true; }

    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; this.dirty = true; }
    public void addDeaths(int amount) { this.deaths += amount; this.dirty = true; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; this.dirty = true; }

    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }

    public double getKdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public double getWinRate() {
        int total = wins + losses;
        return total == 0 ? 0 : (double) wins / total * 100;
    }
}

