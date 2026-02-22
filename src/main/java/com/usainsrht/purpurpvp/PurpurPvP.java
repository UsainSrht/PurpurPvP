package com.usainsrht.purpurpvp;

import com.usainsrht.purpurpvp.arena.ArenaManager;
import com.usainsrht.purpurpvp.chat.ChatManager;
import com.usainsrht.purpurpvp.command.DuelCommand;
import com.usainsrht.purpurpvp.command.PurpurPvPCommand;
import com.usainsrht.purpurpvp.command.QueueCommand;
import com.usainsrht.purpurpvp.config.ConfigManager;
import com.usainsrht.purpurpvp.database.DatabaseManager;
import com.usainsrht.purpurpvp.database.SchemaManager;
import com.usainsrht.purpurpvp.database.repository.KitRepository;
import com.usainsrht.purpurpvp.database.repository.MatchRepository;
import com.usainsrht.purpurpvp.database.repository.PlayerRepository;
import com.usainsrht.purpurpvp.kit.KitManager;
import com.usainsrht.purpurpvp.kit.KitNameListener;
import com.usainsrht.purpurpvp.kit.gui.KitEditorGUI;
import com.usainsrht.purpurpvp.kit.gui.KitSelectorGUI;
import com.usainsrht.purpurpvp.match.MatchListener;
import com.usainsrht.purpurpvp.match.MatchManager;
import com.usainsrht.purpurpvp.queue.QueueManager;
import com.usainsrht.purpurpvp.ranked.RankManager;
import com.usainsrht.purpurpvp.ranked.leaderboard.LeaderboardGUI;
import com.usainsrht.purpurpvp.room.RoomManager;
import com.usainsrht.purpurpvp.room.gui.RoomBrowserGUI;
import com.usainsrht.purpurpvp.room.gui.RoomCreationGUI;
import com.usainsrht.purpurpvp.room.sign.RoomSignManager;
import com.usainsrht.purpurpvp.scoreboard.ScoreboardManager;
import com.usainsrht.purpurpvp.util.LocationSerializer;
import dev.triumphteam.gui.paper.PaperGuiSettings;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PurpurPvP extends JavaPlugin implements Listener {

    // Config & Database
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private KitRepository kitRepository;
    private MatchRepository matchRepository;

    // Core Managers
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private RoomManager roomManager;
    private QueueManager queueManager;
    private RankManager rankManager;
    private ChatManager chatManager;
    private RoomSignManager roomSignManager;
    private ScoreboardManager scoreboardManager;

    // Listeners
    private KitNameListener kitNameListener;

    // GUIs
    private KitEditorGUI kitEditorGUI;
    private KitSelectorGUI kitSelectorGUI;
    private RoomCreationGUI roomCreationGUI;
    private RoomBrowserGUI roomBrowserGUI;
    private LeaderboardGUI leaderboardGUI;

    // Lobby location
    private Location lobbyLocation;

    @Override
    public void onEnable() {
        // Phase 1: Config
        configManager = new ConfigManager(this);

        // Phase 1: Database
        databaseManager = new DatabaseManager(
                configManager.getDatabaseType(),
                getDataFolder(),
                configManager.getDbHost(),
                configManager.getDbPort(),
                configManager.getDbName(),
                configManager.getDbUsername(),
                configManager.getDbPassword(),
                getLogger()
        );
        new SchemaManager(databaseManager, getLogger()).initialize();

        // Repositories
        playerRepository = new PlayerRepository(databaseManager, getLogger());
        kitRepository = new KitRepository(databaseManager, getLogger());
        matchRepository = new MatchRepository(databaseManager, getLogger());

        // Phase 2: Kits & Arenas
        kitManager = new KitManager(kitRepository, getLogger());
        arenaManager = new ArenaManager(this);

        // Phase 3: Match Engine
        matchManager = new MatchManager(this);

        // Phase 4: Rooms & Queues
        roomManager = new RoomManager(this);
        queueManager = new QueueManager(this);

        // Phase 5: Ranked & Chat
        rankManager = new RankManager(this, playerRepository);
        chatManager = new ChatManager(this);
        roomSignManager = new RoomSignManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // GUIs
        PaperGuiSettings.init(this);
        kitEditorGUI = new KitEditorGUI(this);
        kitSelectorGUI = new KitSelectorGUI(this);
        roomCreationGUI = new RoomCreationGUI(this);
        roomBrowserGUI = new RoomBrowserGUI(this);
        leaderboardGUI = new LeaderboardGUI(this);

        // Listeners
        kitNameListener = new KitNameListener(this);
        getServer().getPluginManager().registerEvents(new MatchListener(this), this);
        getServer().getPluginManager().registerEvents(chatManager, this);
        getServer().getPluginManager().registerEvents(roomSignManager, this);
        getServer().getPluginManager().registerEvents(this, this);

        // Commands — Modern Paper Brigadier/Lifecycle API
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var commands = event.registrar();
            commands.register(new DuelCommand(this).buildNode(), "Challenge a player to a duel");
            commands.register(new QueueCommand(this).buildNode(), "Join a matchmaking queue");
            commands.register(new PurpurPvPCommand(this).buildNode(), "PurpurPvP admin commands", java.util.List.of("pvp", "ppvp"));
        });

        // Load lobby location
        String lobbyStr = getConfig().getString("lobby-location");
        if (lobbyStr != null) {
            lobbyLocation = LocationSerializer.deserialize(lobbyStr);
        }

        getLogger().info("PurpurPvP enabled! All systems online.");
    }

    @Override
    public void onDisable() {
        // Shutdown in reverse order
        if (scoreboardManager != null) scoreboardManager.shutdown();
        if (queueManager != null) queueManager.shutdown();
        if (matchManager != null) matchManager.shutdown();
        if (roomSignManager != null) roomSignManager.shutdown();
        if (rankManager != null) rankManager.shutdown();
        if (arenaManager != null) arenaManager.saveArenas();
        if (databaseManager != null) databaseManager.close();

        getLogger().info("PurpurPvP disabled. Goodbye!");
    }

    // ===== Player Join/Quit (profile loading, kit caching) =====

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        rankManager.loadProfile(player.getUniqueId(), player.getName());
        kitManager.loadPlayerKits(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove scoreboard
        if (scoreboardManager != null) scoreboardManager.removeBoard(player);

        // Leave any room/queue
        roomManager.leaveRoom(player.getUniqueId());
        queueManager.leaveQueue(player.getUniqueId());

        // Save and unload profile & kits
        rankManager.unloadProfile(player.getUniqueId());
        kitManager.unloadPlayerKits(player.getUniqueId());
    }

    // ===== Getters =====

    public ConfigManager getConfigManager() { return configManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerRepository getPlayerRepository() { return playerRepository; }
    public KitRepository getKitRepository() { return kitRepository; }
    public MatchRepository getMatchRepository() { return matchRepository; }

    public KitManager getKitManager() { return kitManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public RoomManager getRoomManager() { return roomManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public RankManager getRankManager() { return rankManager; }
    public ChatManager getChatManager() { return chatManager; }
    public RoomSignManager getRoomSignManager() { return roomSignManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }

    public KitNameListener getKitNameListener() { return kitNameListener; }

    public KitEditorGUI getKitEditorGUI() { return kitEditorGUI; }
    public KitSelectorGUI getKitSelectorGUI() { return kitSelectorGUI; }
    public RoomCreationGUI getRoomCreationGUI() { return roomCreationGUI; }
    public RoomBrowserGUI getRoomBrowserGUI() { return roomBrowserGUI; }
    public LeaderboardGUI getLeaderboardGUI() { return leaderboardGUI; }

    public Location getLobbyLocation() { return lobbyLocation; }

    public void setLobbyLocation(Location location) {
        this.lobbyLocation = location;
        getConfig().set("lobby-location", LocationSerializer.serialize(location));
        saveConfig();
    }
}
