package com.usainsrht.purpurpvp.database.repository;

import com.usainsrht.purpurpvp.database.DatabaseManager;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Repository for player profiles (XP, level, rating, stats).
 */
public class PlayerRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public PlayerRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public Optional<PlayerProfile> findByUuid(UUID uuid) {
        String sql = "SELECT * FROM player_profiles WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("[PlayerRepo] Failed to find player: " + e.getMessage());
        }
        return Optional.empty();
    }

    public void saveOrUpdate(PlayerProfile profile) {
        String sql = """
            INSERT INTO player_profiles (uuid, username, xp, level, elo_rating, glicko_rating, glicko_rd, glicko_volatility, wins, losses, kills, deaths, last_seen)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                username = VALUES(username), xp = VALUES(xp), level = VALUES(level),
                elo_rating = VALUES(elo_rating), glicko_rating = VALUES(glicko_rating),
                glicko_rd = VALUES(glicko_rd), glicko_volatility = VALUES(glicko_volatility),
                wins = VALUES(wins), losses = VALUES(losses), kills = VALUES(kills),
                deaths = VALUES(deaths), last_seen = VALUES(last_seen)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, profile.getUuid().toString());
            ps.setString(2, profile.getUsername());
            ps.setLong(3, profile.getXp());
            ps.setInt(4, profile.getLevel());
            ps.setDouble(5, profile.getEloRating());
            ps.setDouble(6, profile.getGlickoRating());
            ps.setDouble(7, profile.getGlickoRd());
            ps.setDouble(8, profile.getGlickoVolatility());
            ps.setInt(9, profile.getWins());
            ps.setInt(10, profile.getLosses());
            ps.setInt(11, profile.getKills());
            ps.setInt(12, profile.getDeaths());
            ps.setLong(13, profile.getLastSeen());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[PlayerRepo] Failed to save player: " + e.getMessage());
        }
    }

    public List<PlayerProfile> getTopByElo(int limit) {
        List<PlayerProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM player_profiles ORDER BY elo_rating DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("[PlayerRepo] Failed to get leaderboard: " + e.getMessage());
        }
        return list;
    }

    public List<PlayerProfile> getTopByLevel(int limit) {
        List<PlayerProfile> list = new ArrayList<>();
        String sql = "SELECT * FROM player_profiles ORDER BY level DESC, xp DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.severe("[PlayerRepo] Failed to get level leaderboard: " + e.getMessage());
        }
        return list;
    }

    private PlayerProfile mapRow(ResultSet rs) throws SQLException {
        PlayerProfile p = new PlayerProfile(UUID.fromString(rs.getString("uuid")));
        p.setUsername(rs.getString("username"));
        p.setXp(rs.getLong("xp"));
        p.setLevel(rs.getInt("level"));
        p.setEloRating(rs.getDouble("elo_rating"));
        p.setGlickoRating(rs.getDouble("glicko_rating"));
        p.setGlickoRd(rs.getDouble("glicko_rd"));
        p.setGlickoVolatility(rs.getDouble("glicko_volatility"));
        p.setWins(rs.getInt("wins"));
        p.setLosses(rs.getInt("losses"));
        p.setKills(rs.getInt("kills"));
        p.setDeaths(rs.getInt("deaths"));
        p.setLastSeen(rs.getLong("last_seen"));
        return p;
    }
}

