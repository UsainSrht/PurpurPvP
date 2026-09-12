package com.usainsrht.purpurpvp.config;

import com.usainsrht.purpurpvp.database.DatabaseType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Loads and provides access to config.yml settings.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Messages
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // ===== Database =====

    public DatabaseType getDatabaseType() {
        String type = config.getString("database.type", "H2").toUpperCase();
        try {
            return DatabaseType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return DatabaseType.H2;
        }
    }

    public String getDbHost() { return config.getString("database.host", "localhost"); }
    public int getDbPort() { return config.getInt("database.port", 3306); }
    public String getDbName() { return config.getString("database.name", "purpurpvp"); }
    public String getDbUsername() { return config.getString("database.username", "root"); }
    public String getDbPassword() { return config.getString("database.password", ""); }

    // ===== Match Settings =====

    public int getDefaultBestOf() { return config.getInt("match.default-best-of", 1); }
    public int getCountdownSeconds() { return config.getInt("match.countdown-seconds", 5); }
    public int getMaxRoomIdleSeconds() { return config.getInt("match.max-room-idle-seconds", 300); }

    // ===== XP Settings =====

    public long getXpPerKill() { return config.getLong("xp.per-kill", 25); }
    public long getXpPerRoundWin() { return config.getLong("xp.per-round-win", 50); }
    public long getXpPerMatchWin() { return config.getLong("xp.per-match-win", 100); }
    public long getXpPerMatchComplete() { return config.getLong("xp.per-match-complete", 30); }
    public long getXpPerLevel() { return config.getLong("xp.per-level", 500); }

    // ===== Elo Settings =====

    public int getEloKFactor() { return config.getInt("elo.k-factor", 32); }

    // ===== Arena Settings =====

    public String getArenaWorld() { return config.getString("arena.world", "purpurpvp_arenas"); }
    public int getArenaGridSpacing() { return config.getInt("arena.grid-spacing", 300); }
    public int getArenaInitialPoolSize() { return config.getInt("arena.initial-pool-size", 5); }

    // ===== Lobby Items =====

    public boolean isLobbyItemsEnabled() { return config.getBoolean("lobby-items.enabled", true); }
    public org.bukkit.configuration.ConfigurationSection getLobbyItemsSection() {
        return config.getConfigurationSection("lobby-items.items");
    }

    // ===== Messages =====

    public String getMessage(String key) {
        return messages.getString(key, "<red>Missing message: " + key);
    }

    public FileConfiguration getConfig() { return config; }
}

