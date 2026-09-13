package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.match.MatchInventorySnapshot;
import com.usainsrht.purpurpvp.match.gui.InventoryInspectGUI;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;

/**
 * /purpurpvp — Admin and utility commands tree via Brigadier.
 * Fully localized with CommandLocalizationManager and YamlMessageAPI.
 */
@SuppressWarnings("UnstableApiUsage")
public class PurpurPvPCommand {

    private final PurpurPvP plugin;

    public PurpurPvPCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        var clm = plugin.getCommandLocalizationManager();
        String cmdName = clm.getName("purpurpvp", "purpurpvp");

        var root = Commands.literal(cmdName)
                .executes(ctx -> { sendHelp(ctx.getSource()); return Command.SINGLE_SUCCESS; });

        // /purpurpvp reload
        registerSub(root, "purpurpvp", "reload", Commands.literal("reload")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .executes(ctx -> {
                    plugin.getConfigManager().reload();
                    plugin.getCommandLocalizationManager().reload();
                    plugin.getMessageService().reload();
                    plugin.getArenaManager().loadArenas();
                    plugin.getDynamicArenaManager().loadTemplates();
                    plugin.getKitManager().loadGlobalKits();
                    if (plugin.getPvPWorldManager() != null) plugin.getPvPWorldManager().reload();
                    if (plugin.getFfaManager() != null) plugin.getFfaManager().loadArenas();
                    if (plugin.getLobbyItemManager() != null) plugin.getLobbyItemManager().loadItems();
                    plugin.getMessageService().send(ctx.getSource().getSender(), "admin.reload");
                    return Command.SINGLE_SUCCESS;
                }));

        // /purpurpvp setlobby
        registerSub(root, "purpurpvp", "setlobby", Commands.literal("setlobby")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        plugin.setLobbyLocation(player.getLocation());
                        plugin.getMessageService().send(player, "admin.setlobby");
                    }
                    return Command.SINGLE_SUCCESS;
                }));

        // /purpurpvp leaderboard
        registerSub(root, "purpurpvp", "leaderboard", Commands.literal("leaderboard")
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) plugin.getLeaderboardGUI().open(player);
                    return Command.SINGLE_SUCCESS;
                }));

        // /purpurpvp inspect <matchId> <player>
        registerSub(root, "purpurpvp", "inspect", Commands.literal("inspect")
                .then(Commands.argument("match", StringArgumentType.word())
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player viewer) {
                                        try {
                                            UUID matchId = UUID.fromString(StringArgumentType.getString(ctx, "match"));
                                            String targetName = StringArgumentType.getString(ctx, "target");
                                            MatchInventorySnapshot snapshot = plugin.getMatchManager().getSnapshot(matchId, targetName);
                                            if (snapshot != null) {
                                                InventoryInspectGUI.open(viewer, snapshot);
                                            } else {
                                                plugin.getMessageService().send(viewer, "error.player-not-found");
                                            }
                                        } catch (IllegalArgumentException e) {
                                            plugin.getMessageService().send(viewer, "error.invalid-args", Placeholder.parsed("usage", "/purpurpvp inspect <matchId> <player>"));
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))));

        // /purpurpvp stats [player]
        registerSub(root, "purpurpvp", "stats", Commands.literal("stats")
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) showStats(player, player);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("target", StringArgumentType.word())
                        .suggests((ctx, b) -> { Bukkit.getOnlinePlayers().forEach(p -> b.suggest(p.getName())); return b.buildFuture(); })
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                Player target = Bukkit.getPlayerExact(StringArgumentType.getString(ctx, "target"));
                                if (target != null) showStats(player, target);
                                else plugin.getMessageService().send(player, "error.player-not-found");
                            }
                            return Command.SINGLE_SUCCESS;
                        })));

        // /purpurpvp arena ...
        registerSub(root, "purpurpvp", "arena", arenaSubcommand());

        // /purpurpvp kit ...
        registerSub(root, "purpurpvp", "kit", kitSubcommand());

        return root.build();
    }

    private void registerSub(LiteralArgumentBuilder<CommandSourceStack> root, String cmdKey, String subKey, LiteralArgumentBuilder<CommandSourceStack> builder) {
        String localized = plugin.getCommandLocalizationManager().getSubcommand(cmdKey, subKey, subKey);
        if (!localized.equalsIgnoreCase(subKey)) {
            var locBuilder = Commands.literal(localized);
            if (builder.getCommand() != null) locBuilder.executes(builder.getCommand());
            builder.getArguments().forEach(locBuilder::then);
            root.then(locBuilder);
        }
        root.then(builder);
    }

    // ===== Arena =====

    private LiteralArgumentBuilder<CommandSourceStack> arenaSubcommand() {
        return Commands.literal("arena")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (plugin.getArenaManager().getArena(name) != null) {
                                        plugin.getMessageService().send(ctx.getSource().getSender(), "error.invalid-args", Placeholder.parsed("usage", "Arena already exists!"));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Arena arena = plugin.getArenaManager().createArena(name);
                                    if (ctx.getSource().getSender() instanceof Player p) {
                                        arena.setWorld(p.getWorld().getName());
                                    } else {
                                        arena.setWorld(plugin.getPvPWorldManager() != null ? plugin.getPvPWorldManager().getDuelWorldName() : "world");
                                    }
                                    plugin.getArenaManager().saveArenas();
                                    plugin.getDynamicArenaManager().loadTemplates();
                                    plugin.getMessageService().send(ctx.getSource().getSender(), "admin.arena-created", Placeholder.parsed("name", name));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    plugin.getArenaManager().deleteArena(name);
                                    plugin.getDynamicArenaManager().loadTemplates();
                                    plugin.getMessageService().send(ctx.getSource().getSender(), "admin.arena-deleted", Placeholder.parsed("name", name));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("setspawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .then(Commands.argument("team", IntegerArgumentType.integer(0))
                                         .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                            String name = StringArgumentType.getString(ctx, "name");
                                            int team = IntegerArgumentType.getInteger(ctx, "team");
                                            Arena arena = plugin.getArenaManager().getArena(name);
                                            if (arena == null) {
                                                plugin.getMessageService().send(player, "error.player-not-found");
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            arena.addTeamSpawnPoint(team, player.getLocation());
                                            plugin.getArenaManager().saveArenas();
                                            plugin.getDynamicArenaManager().loadTemplates();
                                            plugin.getMessageService().send(player, "admin.spawn-set", Placeholder.parsed("team", String.valueOf(team)), Placeholder.parsed("name", name));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("setspec")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Arena arena = plugin.getArenaManager().getArena(name);
                                    if (arena == null) {
                                        plugin.getMessageService().send(player, "error.player-not-found");
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    arena.setSpectatorSpawn(player.getLocation());
                                    plugin.getArenaManager().saveArenas();
                                    plugin.getDynamicArenaManager().loadTemplates();
                                    plugin.getMessageService().send(player, "admin.spec-set", Placeholder.parsed("name", name));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
                            if (arenas.isEmpty()) {
                                plugin.getMessageService().send(ctx.getSource().getSender(), "error.invalid-args", Placeholder.parsed("usage", "No arenas configured."));
                                return Command.SINGLE_SUCCESS;
                            }
                            for (Arena a : arenas) {
                                boolean inUse = plugin.getArenaManager().isInUse(a.getName());
                                plugin.getMessageService().sendWithoutPrefix(ctx.getSource().getSender(), "admin.arena-list-entry",
                                        Placeholder.parsed("name", a.getName()),
                                        Placeholder.parsed("teams", String.valueOf(a.getMaxTeams())),
                                        Placeholder.parsed("status", inUse ? "IN USE" : "Ready"),
                                        Placeholder.parsed("color", inUse ? "<red>" : "<green>"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    // ===== Kit =====

    private LiteralArgumentBuilder<CommandSourceStack> kitSubcommand() {
        return Commands.literal("kit")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Kit kit = plugin.getKitManager().createFromInventory(player, name);
                                    plugin.getKitManager().saveGlobalKit(kit);
                                    plugin.getMessageService().send(player, "kit.created", Placeholder.parsed("name", name));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getKitManager().getAllGlobalKits().forEach(k -> b.suggest(k.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    plugin.getKitManager().deleteGlobalKit(name);
                                    plugin.getMessageService().send(ctx.getSource().getSender(), "kit.deleted", Placeholder.parsed("name", name));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            Collection<Kit> kits = plugin.getKitManager().getAllGlobalKits();
                            for (Kit k : kits) {
                                plugin.getMessageService().sendWithoutPrefix(ctx.getSource().getSender(), "admin.kit-list-entry",
                                        Placeholder.parsed("name", k.getName()));
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    // ===== Helpers =====

    private void showStats(Player viewer, Player target) {
        PlayerProfile profile = plugin.getRankManager().getProfile(target.getUniqueId());
        if (profile == null) {
            plugin.getMessageService().send(viewer, "error.player-not-found");
            return;
        }
        plugin.getMessageService().sendWithoutPrefix(viewer, "stats.header", Placeholder.parsed("username", profile.getUsername()));
        plugin.getMessageService().sendWithoutPrefix(viewer, "stats.level",
                Placeholder.parsed("level", String.valueOf(profile.getLevel())),
                Placeholder.parsed("xp", String.valueOf(profile.getXp())));
        plugin.getMessageService().sendWithoutPrefix(viewer, "stats.elo",
                Placeholder.parsed("elo", String.format("%.0f", profile.getEloRating())));
        plugin.getMessageService().sendWithoutPrefix(viewer, "stats.wl",
                Placeholder.parsed("wins", String.valueOf(profile.getWins())),
                Placeholder.parsed("losses", String.valueOf(profile.getLosses())));
        plugin.getMessageService().sendWithoutPrefix(viewer, "stats.kd",
                Placeholder.parsed("kills", String.valueOf(profile.getKills())),
                Placeholder.parsed("deaths", String.valueOf(profile.getDeaths())));
    }

    private void sendHelp(CommandSourceStack source) {
        var sender = source.getSender();
        plugin.getMessageService().sendWithoutPrefix(sender, "help.header");
        plugin.getMessageService().sendWithoutPrefix(sender, "help.duel");
        plugin.getMessageService().sendWithoutPrefix(sender, "help.queue");
        plugin.getMessageService().sendWithoutPrefix(sender, "help.party");
        plugin.getMessageService().sendWithoutPrefix(sender, "help.stats");
        plugin.getMessageService().sendWithoutPrefix(sender, "help.leaderboard");
        if (sender.hasPermission("purpurpvp.admin")) {
            plugin.getMessageService().sendWithoutPrefix(sender, "help.admin");
        }
    }
}
