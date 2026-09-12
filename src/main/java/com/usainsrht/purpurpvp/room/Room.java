package com.usainsrht.purpurpvp.room;

import com.usainsrht.purpurpvp.match.MatchConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Represents a pre-match room where players gather and the host configures settings.
 */
public class Room {

    private final UUID roomId;
    private final UUID hostUuid;
    private final MatchConfig matchConfig;
    private final List<UUID> players = new ArrayList<>();
    private RoomState state;
    private long createdAt;
    private boolean isPublic;

    public Room(UUID hostUuid) {
        this.roomId = UUID.randomUUID();
        this.hostUuid = hostUuid;
        this.matchConfig = new MatchConfig();
        this.state = RoomState.CONFIGURING;
        this.createdAt = System.currentTimeMillis();
        this.isPublic = true;
        players.add(hostUuid);
    }

    public UUID getRoomId() { return roomId; }
    public UUID getHostUuid() { return hostUuid; }
    public MatchConfig getMatchConfig() { return matchConfig; }
    public RoomState getState() { return state; }
    public void setState(RoomState state) { this.state = state; }
    public long getCreatedAt() { return createdAt; }
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean isPublic) { this.isPublic = isPublic; }

    public List<UUID> getPlayers() { return Collections.unmodifiableList(players); }

    public int getPlayerCount() { return players.size(); }

    public int getMaxPlayers() {
        return matchConfig.getTeamSize() * matchConfig.getTeamCount();
    }

    public boolean isFull() {
        return players.size() >= getMaxPlayers();
    }

    public boolean addPlayer(UUID uuid) {
        if (isFull() || players.contains(uuid)) return false;
        players.add(uuid);
        broadcastMessage("room.joined", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", getPlayerName(uuid)));
        return true;
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
        broadcastMessage("room.left", net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player", getPlayerName(uuid)));
    }

    public boolean isHost(UUID uuid) {
        return hostUuid.equals(uuid);
    }

    public void broadcastMessage(String key, net.kyori.adventure.text.minimessage.tag.resolver.TagResolver... tags) {
        com.usainsrht.purpurpvp.PurpurPvP plugin = org.bukkit.plugin.java.JavaPlugin.getPlugin(com.usainsrht.purpurpvp.PurpurPvP.class);
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) plugin.getMessageService().send(p, key, tags);
        }
    }

    public void broadcastToRoom(Component message) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    private String getPlayerName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : "Unknown";
    }
}

