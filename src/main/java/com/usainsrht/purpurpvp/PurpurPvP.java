package com.usainsrht.purpurpvp;

import com.usainsrht.purpurpvp.arena.ArenaManager;
import com.usainsrht.purpurpvp.arena.DynamicArenaManager;
import com.usainsrht.purpurpvp.chat.ChatManager;
import com.usainsrht.purpurpvp.command.CommandLocalizationManager;
import com.usainsrht.purpurpvp.command.DuelCommand;
import com.usainsrht.purpurpvp.command.FfaCommand;
import com.usainsrht.purpurpvp.command.PartyCommand;
import com.usainsrht.purpurpvp.command.PurpurPvPCommand;
import com.usainsrht.purpurpvp.command.QueueCommand;
import com.usainsrht.purpurpvp.config.ConfigManager;
import com.usainsrht.purpurpvp.database.DatabaseManager;
import com.usainsrht.purpurpvp.database.SchemaManager;
import com.usainsrht.purpurpvp.database.repository.KitRepository;
import com.usainsrht.purpurpvp.database.repository.MatchRepository;
import com.usainsrht.purpurpvp.database.repository.PlayerRepository;
import com.usainsrht.purpurpvp.dialog.DialogService;
import com.usainsrht.purpurpvp.ffa.FfaManager;
import com.usainsrht.purpurpvp.kit.KitManager;
import com.usainsrht.purpurpvp.kit.KitNameListener;
import com.usainsrht.purpurpvp.kit.gui.KitEditorGUI;
import com.usainsrht.purpurpvp.kit.gui.KitSelectorGUI;
import com.usainsrht.purpurpvp.lobby.LobbyInventoryManager;
import com.usainsrht.purpurpvp.lobby.LobbyItemManager;
import com.usainsrht.purpurpvp.match.MatchListener;
import com.usainsrht.purpurpvp.match.MatchManager;
import com.usainsrht.purpurpvp.message.MessageService;
import com.usainsrht.purpurpvp.party.PartyManager;
import com.usainsrht.purpurpvp.queue.QueueManager;
import com.usainsrht.purpurpvp.ranked.RankManager;
import com.usainsrht.purpurpvp.ranked.leaderboard.LeaderboardGUI;
import com.usainsrht.purpurpvp.room.RoomManager;
import com.usainsrht.purpurpvp.room.gui.RoomBrowserGUI;
import com.usainsrht.purpurpvp.room.gui.RoomCreationGUI;
import com.usainsrht.purpurpvp.room.sign.RoomSignManager;
import com.usainsrht.purpurpvp.scoreboard.ScoreboardManager;
import com.usainsrht.purpurpvp.util.LocationSerializer;
import com.usainsrht.purpurpvp.world.PvPWorldManager;
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
    private CommandLocalizationManager commandLocalizationManager;
    private MessageService messageService;
    private DatabaseManager databaseManager;
    private PlayerRepository playerRepository;
    private KitRepository kitRepository;
    private MatchRepository matchRepository;

    // Core Managers & Services
    private DialogService dialogService;
    private PvPWorldManager pvpWorldManager;
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private DynamicArenaManager dynamicArenaManager;
    private FfaManager ffaManager;
    private MatchManager matchManager;
    private RoomManager roomManager;
    private QueueManager queueManager;
    private RankManager rankManager;
    private ChatManager chatManager;
    private RoomSignManager roomSignManager;
    private ScoreboardManager scoreboardManager;
    private LobbyInventoryManager lobbyInventoryManager;
    private LobbyItemManager lobbyItemManager;
    private PartyManager partyManager;

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
        // Phase 1: Config, Localization, Messages, Dialog & World
        configManager = new ConfigManager(this);
        commandLocalizationManager = new CommandLocalizationManager(this);
        messageService = new MessageService(this);
        dialogService = new DialogService();
        pvpWorldManager = new PvPWorldManager(this);

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

        // Phase 2: Kits, Arenas & FFA
        kitManager = new KitManager(kitRepository, getLogger());
        arenaManager = new ArenaManager(this);
        dynamicArenaManager = new DynamicArenaManager(this);
        ffaManager = new FfaManager(this);

        // Phase 3: Match Engine
        matchManager = new MatchManager(this);

        // Phase 4: Rooms & Queues
        roomManager = new RoomManager(this);
        queueManager = new QueueManager(this);

        // Phase 5: Ranked, Chat, Lobby & Party
        rankManager = new RankManager(this, playerRepository);
        chatManager = new ChatManager(this);
        roomSignManager = new RoomSignManager(this);
        scoreboardManager = new ScoreboardManager(this);
        lobbyInventoryManager = new LobbyInventoryManager(this);
        lobbyItemManager = new LobbyItemManager(this);
        partyManager = new PartyManager(this);

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
        getServer().getPluginManager().registerEvents(lobbyInventoryManager, this);
        getServer().getPluginManager().registerEvents(lobbyItemManager, this);
        getServer().getPluginManager().registerEvents(ffaManager, this);
        getServer().getPluginManager().registerEvents(this, this);

        // Commands — Modern Paper Brigadier/Lifecycle API with 100% localization
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            var commands = event.registrar();
            var clm = commandLocalizationManager;
            commands.register(new DuelCommand(this).buildNode(), clm.getDescription("duel", "Challenge a player to a duel"), clm.getAliases("duel"));
            commands.register(new QueueCommand(this).buildNode(), clm.getDescription("queue", "Join a matchmaking queue"), clm.getAliases("queue"));
            commands.register(new PartyCommand(this).buildNode(), clm.getDescription("party", "Party management commands"), clm.getAliases("party"));
            commands.register(new PurpurPvPCommand(this).buildNode(), clm.getDescription("purpurpvp", "PurpurPvP admin commands"), clm.getAliases("purpurpvp"));
            commands.register(new FfaCommand(this).buildNode(), clm.getDescription("ffa", "Join or leave public FFA battle arenas"), clm.getAliases("ffa"));
        });

        // Load lobby location
        String lobbyStr = getConfig().getString("lobby-location");
        if (lobbyStr != null) {
            lobbyLocation = LocationSerializer.deserialize(lobbyStr);
        } else if (pvpWorldManager.getLobbyWorld() != null) {
            lobbyLocation = pvpWorldManager.getLobbyWorld().getSpawnLocation();
        }

        getLogger().info("PurpurPvP enabled! All systems online.");
    }

    @Override
    public void onDisable() {
        // Shutdown in reverse order
        if (ffaManager != null) ffaManager.saveArenas();
        if (partyManager != null) partyManager.disbandAll();
        if (scoreboardManager != null) scoreboardManager.shutdown();
        if (queueManager != null) queueManager.shutdown();
        if (matchManager != null) matchManager.shutdown();
        if (roomSignManager != null) roomSignManager.shutdown();
        if (rankManager != null) rankManager.shutdown();
        if (dynamicArenaManager != null) dynamicArenaManager.shutdown();
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

        // Leave any party, room, queue, or FFA
        if (partyManager != null) partyManager.leaveParty(player);
        if (ffaManager != null) ffaManager.leaveFfa(player);
        roomManager.leaveRoom(player.getUniqueId());
        queueManager.leaveQueue(player.getUniqueId());

        // Save and unload profile & kits
        rankManager.unloadProfile(player.getUniqueId());
        kitManager.unloadPlayerKits(player.getUniqueId());
    }

    // ===== Getters =====

    public ConfigManager getConfigManager() { return configManager; }
    public CommandLocalizationManager getCommandLocalizationManager() { return commandLocalizationManager; }
    public MessageService getMessageService() { return messageService; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public PlayerRepository getPlayerRepository() { return playerRepository; }
    public KitRepository getKitRepository() { return kitRepository; }
    public MatchRepository getMatchRepository() { return matchRepository; }

    public DialogService getDialogService() { return dialogService; }
    public PvPWorldManager getPvPWorldManager() { return pvpWorldManager; }
    public KitManager getKitManager() { return kitManager; }
    public ArenaManager getArenaManager() { return arenaManager; }
    public DynamicArenaManager getDynamicArenaManager() { return dynamicArenaManager; }
    public FfaManager getFfaManager() { return ffaManager; }
    public MatchManager getMatchManager() { return matchManager; }
    public RoomManager getRoomManager() { return roomManager; }
    public QueueManager getQueueManager() { return queueManager; }
    public RankManager getRankManager() { return rankManager; }
    public ChatManager getChatManager() { return chatManager; }
    public RoomSignManager getRoomSignManager() { return roomSignManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public LobbyInventoryManager getLobbyInventoryManager() { return lobbyInventoryManager; }
    public LobbyItemManager getLobbyItemManager() { return lobbyItemManager; }
    public PartyManager getPartyManager() { return partyManager; }

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
