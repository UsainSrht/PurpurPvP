package com.usainsrht.purpurpvp.party;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates a player party for team matchmaking, party vs party duels, and internal practice.
 */
public class Party {

    private final UUID partyId;
    private UUID leaderUuid;
    private final Set<UUID> members = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> invites = new ConcurrentHashMap<>(); // invited player UUID -> timestamp
    private boolean open;

    public Party(UUID leaderUuid) {
        this.partyId = UUID.randomUUID();
        this.leaderUuid = leaderUuid;
        this.members.add(leaderUuid);
        this.open = false;
    }

    public boolean isLeader(UUID uuid) {
        return leaderUuid.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public boolean addMember(UUID uuid) {
        invites.remove(uuid);
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public void invite(UUID uuid) {
        invites.put(uuid, System.currentTimeMillis());
    }

    public boolean hasInvite(UUID uuid) {
        Long time = invites.get(uuid);
        if (time == null) return false;
        if (System.currentTimeMillis() - time > 60_000) {
            invites.remove(uuid);
            return false;
        }
        return true;
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid);
    }

    public UUID getPartyId() { return partyId; }
    public UUID getLeaderUuid() { return leaderUuid; }
    public void setLeaderUuid(UUID leaderUuid) { this.leaderUuid = leaderUuid; }
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }
    public int getSize() { return members.size(); }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
}
