package com.usainsrht.purpurpvp.database.repository;

import com.usainsrht.purpurpvp.database.DatabaseManager;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Repository for match history persistence.
 */
public class MatchRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public MatchRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void saveMatch(UUID matchId, String arenaName, String kitName, int teamSize,
                          int teamCount, int winnerTeam, int roundsPlayed,
                          int durationSeconds, boolean ranked, long startedAt, long endedAt) {
        String sql = """
            INSERT INTO match_history (match_id, arena_name, kit_name, team_size, team_count,
                winner_team, rounds_played, duration_seconds, ranked, started_at, ended_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matchId.toString());
            ps.setString(2, arenaName);
            ps.setString(3, kitName);
            ps.setInt(4, teamSize);
            ps.setInt(5, teamCount);
            ps.setInt(6, winnerTeam);
            ps.setInt(7, roundsPlayed);
            ps.setInt(8, durationSeconds);
            ps.setBoolean(9, ranked);
            ps.setLong(10, startedAt);
            ps.setLong(11, endedAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[MatchRepo] Failed to save match: " + e.getMessage());
        }
    }

    public void saveParticipant(UUID matchId, UUID playerUuid, int teamIndex,
                                int kills, int deaths, double eloChange, long xpEarned) {
        String sql = """
            INSERT INTO match_participants (match_id, uuid, team_index, kills, deaths, elo_change, xp_earned)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, matchId.toString());
            ps.setString(2, playerUuid.toString());
            ps.setInt(3, teamIndex);
            ps.setInt(4, kills);
            ps.setInt(5, deaths);
            ps.setDouble(6, eloChange);
            ps.setLong(7, xpEarned);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[MatchRepo] Failed to save participant: " + e.getMessage());
        }
    }
}

