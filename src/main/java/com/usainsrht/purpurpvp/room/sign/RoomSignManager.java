package com.usainsrht.purpurpvp.room.sign;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.room.Room;
import com.usainsrht.purpurpvp.util.LocationSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages clickable room signs that players can use to join/browse rooms.
 */
public class RoomSignManager implements Listener {

    private final PurpurPvP plugin;
    private final Set<Location> roomSigns = new HashSet<>();
    private BukkitTask updateTask;

    public RoomSignManager(PurpurPvP plugin) {
        this.plugin = plugin;
        loadSigns();
        startUpdateTask();
    }

    private void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllSigns, 40L, 40L);
    }

    /**
     * When a player places a sign with [PvP] on the first line, register it.
     */
    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.line(0) == null) return;
        String firstLine = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(event.line(0));
        if (firstLine.equalsIgnoreCase("[PvP]")) {
            if (!event.getPlayer().hasPermission("purpurpvp.admin.sign")) {
                event.getPlayer().sendMessage(Component.text("No permission to create PvP signs!", NamedTextColor.RED));
                return;
            }
            Location loc = event.getBlock().getLocation();
            roomSigns.add(loc);
            saveSigns();
            event.getPlayer().sendMessage(Component.text("PvP room sign created!", NamedTextColor.GREEN));
            updateSign(loc);
        }
    }

    /**
     * When a player right-clicks a room sign, open the room browser.
     */
    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getState() instanceof Sign)) return;

        if (roomSigns.contains(block.getLocation())) {
            event.setCancelled(true);
            plugin.getRoomBrowserGUI().open(event.getPlayer());
        }
    }

    private void updateAllSigns() {
        Iterator<Location> it = roomSigns.iterator();
        while (it.hasNext()) {
            Location loc = it.next();
            if (loc.getWorld() == null || !(loc.getBlock().getState() instanceof Sign)) {
                it.remove();
                continue;
            }
            updateSign(loc);
        }
    }

    private void updateSign(Location loc) {
        if (!(loc.getBlock().getState() instanceof Sign sign)) return;

        Collection<Room> rooms = plugin.getRoomManager().getPublicRooms();
        int openRooms = rooms.size();
        int activePlayers = 0;
        for (var match : plugin.getMatchManager().getActiveMatches()) {
            activePlayers += match.getAllPlayerUUIDs().size();
        }

        sign.getSide(Side.FRONT).line(0, Component.text("[PurpurPvP]", NamedTextColor.DARK_PURPLE));
        sign.getSide(Side.FRONT).line(1, Component.text("Click to Play!", NamedTextColor.GREEN));
        sign.getSide(Side.FRONT).line(2, Component.text("Rooms: " + openRooms, NamedTextColor.GOLD));
        sign.getSide(Side.FRONT).line(3, Component.text("Fighting: " + activePlayers, NamedTextColor.AQUA));
        sign.update(true);
    }

    // ===== Persistence =====

    private void loadSigns() {
        File file = new File(plugin.getDataFolder(), "signs.yml");
        if (!file.exists()) return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> list = config.getStringList("signs");
        for (String s : list) {
            Location loc = LocationSerializer.deserialize(s);
            if (loc != null) roomSigns.add(loc);
        }
    }

    private void saveSigns() {
        File file = new File(plugin.getDataFolder(), "signs.yml");
        FileConfiguration config = new YamlConfiguration();
        List<String> list = new ArrayList<>();
        for (Location loc : roomSigns) {
            list.add(LocationSerializer.serialize(loc));
        }
        config.set("signs", list);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("[RoomSignManager] Failed to save signs: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        saveSigns();
    }
}


