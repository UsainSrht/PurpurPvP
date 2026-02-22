package com.usainsrht.purpurpvp.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Creates and upgrades database schema.
 */
public class SchemaManager {

    private final DatabaseManager db;
    private final Logger logger;

    public SchemaManager(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    public void initialize() {
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {

            // Player profiles: XP, level, Elo, Glicko-2
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_profiles (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    xp BIGINT DEFAULT 0,
                    level INT DEFAULT 1,
                    elo_rating DOUBLE DEFAULT 1000.0,
                    glicko_rating DOUBLE DEFAULT 1500.0,
                    glicko_rd DOUBLE DEFAULT 350.0,
                    glicko_volatility DOUBLE DEFAULT 0.06,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    last_seen BIGINT DEFAULT 0
                )
            """);

            // Player custom kits
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_kits (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    kit_name VARCHAR(64) NOT NULL,
                    contents TEXT NOT NULL,
                    armor TEXT,
                    offhand TEXT,
                    icon TEXT,
                    created_at BIGINT DEFAULT 0,
                    UNIQUE(uuid, kit_name)
                )
            """);

            // Global admin kits
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS global_kits (
                    kit_name VARCHAR(64) PRIMARY KEY,
                    contents TEXT NOT NULL,
                    armor TEXT,
                    offhand TEXT,
                    icon TEXT,
                    permission VARCHAR(128)
                )
            """);

            // Match history
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS match_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    match_id VARCHAR(36) NOT NULL,
                    arena_name VARCHAR(64),
                    kit_name VARCHAR(64),
                    team_size INT DEFAULT 1,
                    team_count INT DEFAULT 2,
                    winner_team INT DEFAULT -1,
                    rounds_played INT DEFAULT 1,
                    duration_seconds INT DEFAULT 0,
                    ranked BOOLEAN DEFAULT FALSE,
                    started_at BIGINT DEFAULT 0,
                    ended_at BIGINT DEFAULT 0
                )
            """);

            // Match participants
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS match_participants (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    match_id VARCHAR(36) NOT NULL,
                    uuid VARCHAR(36) NOT NULL,
                    team_index INT DEFAULT 0,
                    kills INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    elo_change DOUBLE DEFAULT 0.0,
                    xp_earned BIGINT DEFAULT 0
                )
            """);

            logger.info("[Database] Schema initialized successfully.");
        } catch (SQLException e) {
            logger.severe("[Database] Failed to initialize schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

