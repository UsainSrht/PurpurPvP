package com.usainsrht.purpurpvp.match;

import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;

/**
 * Configuration DTO for creating a match.
 * All game rules are delegated to the {@link GameRules} object.
 */
public class MatchConfig {

    private Kit kit;
    private Arena arena;
    private com.usainsrht.purpurpvp.arena.ArenaInstance arenaInstance;
    private com.usainsrht.purpurpvp.arena.ArenaTemplate arenaTemplate;
    private int teamSize;
    private int teamCount;
    private boolean ranked;
    private final GameRules gameRules;

    public MatchConfig() {
        this.teamSize = 1;
        this.teamCount = 2;
        this.ranked = false;
        this.gameRules = new GameRules();
    }

    // ===== Core Getters/Setters =====

    public Kit getKit() { return kit; }
    public void setKit(Kit kit) { this.kit = kit; }

    public Arena getArena() { return arena; }
    public void setArena(Arena arena) { this.arena = arena; }

    public com.usainsrht.purpurpvp.arena.ArenaInstance getArenaInstance() { return arenaInstance; }
    public void setArenaInstance(com.usainsrht.purpurpvp.arena.ArenaInstance arenaInstance) { this.arenaInstance = arenaInstance; }

    public com.usainsrht.purpurpvp.arena.ArenaTemplate getArenaTemplate() { return arenaTemplate; }
    public void setArenaTemplate(com.usainsrht.purpurpvp.arena.ArenaTemplate arenaTemplate) { this.arenaTemplate = arenaTemplate; }

    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int teamSize) { this.teamSize = teamSize; }

    public int getTeamCount() { return teamCount; }
    public void setTeamCount(int teamCount) { this.teamCount = teamCount; }

    public boolean isRanked() { return ranked; }
    public void setRanked(boolean ranked) { this.ranked = ranked; }

    public GameRules getGameRules() { return gameRules; }

    /**
     * Number of round wins needed to win the match.
     */
    public int getWinsNeeded() {
        return gameRules.getRoundsToWin();
    }

    /**
     * Best-of = 2 * roundsToWin - 1 (maximum rounds possible).
     */
    public int getBestOf() {
        return gameRules.getRoundsToWin() * 2 - 1;
    }
}

