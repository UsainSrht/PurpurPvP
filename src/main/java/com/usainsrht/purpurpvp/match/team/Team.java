package com.usainsrht.purpurpvp.match.team;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a team in a match.
 */
public class Team {

    private final int index;
    private final String name;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private int roundWins = 0;

    public Team(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public Team(int index, String name, Collection<UUID> initialMembers) {
        this(index, name);
        if (initialMembers != null) {
            for (UUID uuid : initialMembers) {
                addMember(uuid);
            }
        }
    }

    public int getIndex() { return index; }
    public String getName() { return name; }

    public void addMember(UUID uuid) {
        members.add(uuid);
        alive.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        alive.remove(uuid);
    }

    public void markDead(UUID uuid) {
        alive.remove(uuid);
    }

    public void resetAlive() {
        alive.clear();
        alive.addAll(members);
    }

    public boolean isAlive(UUID uuid) { return alive.contains(uuid); }
    public boolean hasAlivePlayers() { return !alive.isEmpty(); }
    public int getAliveCount() { return alive.size(); }

    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public Set<UUID> getAlive() { return Collections.unmodifiableSet(alive); }

    public int getRoundWins() { return roundWins; }
    public void incrementRoundWins() { roundWins++; }
    public void resetRoundWins() { roundWins = 0; }

    public List<Player> getOnlinePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }

    public List<Player> getOnlineAlivePlayers() {
        List<Player> players = new ArrayList<>();
        for (UUID uuid : alive) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) players.add(p);
        }
        return players;
    }
}

