package com.usainsrht.purpurpvp.queue;

import java.util.UUID;

/**
 * Represents a player in a matchmaking queue.
 */
public class QueueEntry {

    private final UUID playerUuid;
    private final double rating;
    private final long joinedAt;

    public QueueEntry(UUID playerUuid, double rating) {
        this.playerUuid = playerUuid;
        this.rating = rating;
        this.joinedAt = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public double getRating() { return rating; }
    public long getJoinedAt() { return joinedAt; }

    /**
     * How long this player has been waiting (ms).
     */
    public long getWaitTime() {
        return System.currentTimeMillis() - joinedAt;
    }
}

