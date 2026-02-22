package com.usainsrht.purpurpvp.database.repository;

import com.usainsrht.purpurpvp.database.DatabaseManager;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.util.ItemSerializer;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Repository for kit persistence (both player-custom and global kits).
 */
public class KitRepository {

    private final DatabaseManager db;
    private final Logger logger;

    public KitRepository(DatabaseManager db, Logger logger) {
        this.db = db;
        this.logger = logger;
    }

    // ============== Player Custom Kits ==============

    public List<Kit> getPlayerKits(UUID playerUuid) {
        List<Kit> kits = new ArrayList<>();
        String sql = "SELECT * FROM player_kits WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                kits.add(mapPlayerKit(rs));
            }
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to load player kits: " + e.getMessage());
        }
        return kits;
    }

    public void savePlayerKit(UUID playerUuid, Kit kit) {
        String sql = """
            INSERT INTO player_kits (uuid, kit_name, contents, armor, offhand, icon, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE contents = VALUES(contents), armor = VALUES(armor),
                offhand = VALUES(offhand), icon = VALUES(icon)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, kit.getName());
            ps.setString(3, ItemSerializer.toBase64(kit.getContents()));
            ps.setString(4, ItemSerializer.toBase64(kit.getArmor()));
            ps.setString(5, kit.getOffhand() != null ? ItemSerializer.itemToBase64(kit.getOffhand()) : null);
            ps.setString(6, kit.getIcon() != null ? ItemSerializer.itemToBase64(kit.getIcon()) : null);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to save player kit: " + e.getMessage());
        }
    }

    public void deletePlayerKit(UUID playerUuid, String kitName) {
        String sql = "DELETE FROM player_kits WHERE uuid = ? AND kit_name = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, kitName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to delete player kit: " + e.getMessage());
        }
    }

    // ============== Global Admin Kits ==============

    public List<Kit> getGlobalKits() {
        List<Kit> kits = new ArrayList<>();
        String sql = "SELECT * FROM global_kits";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                kits.add(mapGlobalKit(rs));
            }
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to load global kits: " + e.getMessage());
        }
        return kits;
    }

    public void saveGlobalKit(Kit kit) {
        String sql = """
            INSERT INTO global_kits (kit_name, contents, armor, offhand, icon, permission)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE contents = VALUES(contents), armor = VALUES(armor),
                offhand = VALUES(offhand), icon = VALUES(icon), permission = VALUES(permission)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kit.getName());
            ps.setString(2, ItemSerializer.toBase64(kit.getContents()));
            ps.setString(3, ItemSerializer.toBase64(kit.getArmor()));
            ps.setString(4, kit.getOffhand() != null ? ItemSerializer.itemToBase64(kit.getOffhand()) : null);
            ps.setString(5, kit.getIcon() != null ? ItemSerializer.itemToBase64(kit.getIcon()) : null);
            ps.setString(6, kit.getPermission());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to save global kit: " + e.getMessage());
        }
    }

    public void deleteGlobalKit(String kitName) {
        String sql = "DELETE FROM global_kits WHERE kit_name = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kitName);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.severe("[KitRepo] Failed to delete global kit: " + e.getMessage());
        }
    }

    // ============== Mappers ==============

    private Kit mapPlayerKit(ResultSet rs) throws SQLException {
        Kit kit = new Kit(rs.getString("kit_name"));
        kit.setContents(ItemSerializer.fromBase64(rs.getString("contents")));
        String armorStr = rs.getString("armor");
        if (armorStr != null) kit.setArmor(ItemSerializer.fromBase64(armorStr));
        String offhandStr = rs.getString("offhand");
        if (offhandStr != null) kit.setOffhand(ItemSerializer.itemFromBase64(offhandStr));
        String iconStr = rs.getString("icon");
        if (iconStr != null) kit.setIcon(ItemSerializer.itemFromBase64(iconStr));
        return kit;
    }

    private Kit mapGlobalKit(ResultSet rs) throws SQLException {
        Kit kit = mapPlayerKit(rs);
        kit.setPermission(rs.getString("permission"));
        kit.setGlobal(true);
        return kit;
    }
}

