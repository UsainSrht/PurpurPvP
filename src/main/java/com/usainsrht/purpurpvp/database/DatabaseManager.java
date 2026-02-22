package com.usainsrht.purpurpvp.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Manages the database connection pool via HikariCP.
 * Supports H2 (local file) and MySQL.
 */
public class DatabaseManager {

    private final HikariDataSource dataSource;
    private final DatabaseType type;
    private final Logger logger;

    public DatabaseManager(DatabaseType type, File dataFolder, String host, int port,
                           String database, String username, String password, Logger logger) {
        this.type = type;
        this.logger = logger;

        HikariConfig config = new HikariConfig();
        config.setPoolName("PurpurPvP-Pool");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        switch (type) {
            case H2 -> {
                String dbPath = new File(dataFolder, "purpurpvp").getAbsolutePath();
                config.setJdbcUrl("jdbc:h2:file:" + dbPath + ";MODE=MySQL");
                config.setDriverClassName("org.h2.Driver");
                config.setUsername("sa");
                config.setPassword("");
            }
            case MYSQL -> {
                config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database +
                        "?useSSL=false&allowPublicKeyRetrieval=true&autoReconnect=true");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
                config.setUsername(username);
                config.setPassword(password);
            }
        }

        this.dataSource = new HikariDataSource(config);
        logger.info("[Database] Connection pool initialized (" + type.name() + ")");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DatabaseType getType() {
        return type;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[Database] Connection pool closed.");
        }
    }
}

