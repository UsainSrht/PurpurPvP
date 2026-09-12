package com.usainsrht.purpurpvp.room;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.match.Match;
import com.usainsrht.purpurpvp.match.MatchConfig;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.match.team.TeamAllocator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pre-match rooms: creation, joining, starting.
 */
public class RoomManager {

    private final PurpurPvP plugin;
    private final Map<UUID, Room> rooms = new ConcurrentHashMap<>();       // roomId -> Room
    private final Map<UUID, UUID> playerRoomMap = new ConcurrentHashMap<>(); // playerUUID -> roomId

    public RoomManager(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates a new room with the given player as host.
     */
    public Room createRoom(UUID hostUuid) {
        // Can't create room if already in one
        if (playerRoomMap.containsKey(hostUuid)) return null;
        if (plugin.getMatchManager().isInMatch(hostUuid)) return null;

        Room room = new Room(hostUuid);
        rooms.put(room.getRoomId(), room);
        playerRoomMap.put(hostUuid, room.getRoomId());
        return room;
    }

    /**
     * Player joins an existing room.
     */
    public boolean joinRoom(UUID playerUuid, UUID roomId) {
        if (playerRoomMap.containsKey(playerUuid)) return false;
        if (plugin.getMatchManager().isInMatch(playerUuid)) return false;

        Room room = rooms.get(roomId);
        if (room == null || room.getState() != RoomState.CONFIGURING) return false;

        if (room.addPlayer(playerUuid)) {
            playerRoomMap.put(playerUuid, roomId);
            checkAutoStart(room);
            return true;
        }
        return false;
    }

    /**
     * Player leaves their current room.
     */
    public void leaveRoom(UUID playerUuid) {
        UUID roomId = playerRoomMap.remove(playerUuid);
        if (roomId == null) return;

        Room room = rooms.get(roomId);
        if (room == null) return;

        room.removePlayer(playerUuid);

        // If host left, close the room
        if (room.isHost(playerUuid) || room.getPlayerCount() == 0) {
            closeRoom(roomId);
        }
    }

    /**
     * Closes a room and notifies all players.
     */
    public void closeRoom(UUID roomId) {
        Room room = rooms.remove(roomId);
        if (room == null) return;

        room.setState(RoomState.CLOSED);
        room.broadcastToRoom(plugin.getMessageService().toComponent("room.closed"));

        for (UUID uuid : room.getPlayers()) {
            playerRoomMap.remove(uuid);
        }
    }

    /**
     * Starts the match from a room (host action).
     */
    public boolean startRoom(UUID roomId) {
        Room room = rooms.get(roomId);
        if (room == null || room.getState() != RoomState.CONFIGURING) return false;

        MatchConfig config = room.getMatchConfig();

        // Validate kit
        if (config.getKit() == null) {
            room.broadcastToRoom(plugin.getMessageService().toComponent("room.kit-required"));
            return false;
        }

        // Dynamically acquire an isolated arena instance
        String templateName = config.getArena() != null ? config.getArena().getName() : "default";
        var instance = plugin.getDynamicArenaManager().acquireInstance(templateName);
        config.setArenaInstance(instance);

        room.setState(RoomState.IN_MATCH);

        // Allocate teams
        List<Team> teams = TeamAllocator.allocateRandom(
                room.getPlayers(), config.getTeamCount(), config.getTeamSize());

        // Create match
        Match match = plugin.getMatchManager().createMatch(config, teams);

        // Clean up room
        for (UUID uuid : room.getPlayers()) {
            playerRoomMap.remove(uuid);
        }
        rooms.remove(roomId);

        return true;
    }

    /**
     * Auto-start logic: for 1v1, start immediately when 2 players are in.
     */
    private void checkAutoStart(Room room) {
        if (room.isFull()) {
            MatchConfig config = room.getMatchConfig();
            if (config.getTeamSize() == 1 && config.getTeamCount() == 2) {
                // 1v1: auto-start if kit is selected
                if (config.getKit() != null) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> startRoom(room.getRoomId()), 20L);
                }
            }
        }
    }

    // ===== Query Methods =====

    public Room getRoom(UUID roomId) { return rooms.get(roomId); }

    public Room getRoomOfPlayer(UUID playerUuid) {
        UUID roomId = playerRoomMap.get(playerUuid);
        return roomId != null ? rooms.get(roomId) : null;
    }

    public boolean isInRoom(UUID playerUuid) {
        return playerRoomMap.containsKey(playerUuid);
    }

    public Collection<Room> getPublicRooms() {
        return rooms.values().stream()
                .filter(r -> r.isPublic() && r.getState() == RoomState.CONFIGURING)
                .toList();
    }

    public Collection<Room> getAllRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }
}


