package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * /purpurpvp — Admin and utility commands tree via Brigadier.
 */
@SuppressWarnings("UnstableApiUsage")
public class PurpurPvPCommand {

    private final PurpurPvP plugin;

    public PurpurPvPCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("purpurpvp")
                .executes(ctx -> { sendHelp(ctx.getSource()); return Command.SINGLE_SUCCESS; })

                // /purpurpvp reload
                .then(Commands.literal("reload")
                        .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                        .executes(ctx -> {
                            plugin.getConfigManager().reload();
                            plugin.getArenaManager().loadArenas();
                            plugin.getKitManager().loadGlobalKits();
                            ctx.getSource().getSender().sendMessage(Component.text("Config reloaded!", NamedTextColor.GREEN));
                            return Command.SINGLE_SUCCESS;
                        }))

                // /purpurpvp setlobby
                .then(Commands.literal("setlobby")
                        .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                plugin.setLobbyLocation(player.getLocation());
                                player.sendMessage(Component.text("Lobby set!", NamedTextColor.GREEN));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))

                // /purpurpvp leaderboard
                .then(Commands.literal("leaderboard")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) plugin.getLeaderboardGUI().open(player);
                            return Command.SINGLE_SUCCESS;
                        }))

                // /purpurpvp stats [player]
                .then(Commands.literal("stats")
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
                                        else player.sendMessage(Component.text("Player not found!", NamedTextColor.RED));
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))

                // /purpurpvp arena ...
                .then(arenaSubcommand())

                // /purpurpvp kit ...
                .then(kitSubcommand())

                .build();
    }

    // ===== Arena =====

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> arenaSubcommand() {
        return Commands.literal("arena")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (plugin.getArenaManager().getArena(name) != null) {
                                        ctx.getSource().getSender().sendMessage(Component.text("Already exists!", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    Arena arena = plugin.getArenaManager().createArena(name);
                                    if (ctx.getSource().getSender() instanceof Player p) arena.setWorld(p.getWorld().getName());
                                    plugin.getArenaManager().saveArenas();
                                    ctx.getSource().getSender().sendMessage(Component.text("Arena '" + name + "' created!", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    plugin.getArenaManager().deleteArena(StringArgumentType.getString(ctx, "name"));
                                    ctx.getSource().getSender().sendMessage(Component.text("Deleted!", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("setspawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .then(Commands.argument("team", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                            Arena arena = plugin.getArenaManager().getArena(StringArgumentType.getString(ctx, "name"));
                                            if (arena == null) { player.sendMessage(Component.text("Not found!", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                                            arena.addTeamSpawnPoint(IntegerArgumentType.getInteger(ctx, "team"), player.getLocation());
                                            plugin.getArenaManager().saveArenas();
                                            player.sendMessage(Component.text("Spawn set!", NamedTextColor.GREEN));
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .then(Commands.literal("setspec")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getArenaManager().getAllArenas().forEach(a -> b.suggest(a.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                    Arena arena = plugin.getArenaManager().getArena(StringArgumentType.getString(ctx, "name"));
                                    if (arena == null) { player.sendMessage(Component.text("Not found!", NamedTextColor.RED)); return Command.SINGLE_SUCCESS; }
                                    arena.setSpectatorSpawn(player.getLocation());
                                    plugin.getArenaManager().saveArenas();
                                    player.sendMessage(Component.text("Spectator spawn set!", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
                            if (arenas.isEmpty()) { ctx.getSource().getSender().sendMessage(Component.text("No arenas.", NamedTextColor.YELLOW)); return Command.SINGLE_SUCCESS; }
                            ctx.getSource().getSender().sendMessage(Component.text("--- Arenas ---", NamedTextColor.GOLD));
                            for (Arena a : arenas) {
                                boolean inUse = plugin.getArenaManager().isInUse(a.getName());
                                ctx.getSource().getSender().sendMessage(Component.text("  " + a.getName(), NamedTextColor.WHITE)
                                        .append(Component.text(" | Teams: " + a.getMaxTeams(), NamedTextColor.GRAY))
                                        .append(Component.text(" | " + (inUse ? "IN USE" : "Available"), inUse ? NamedTextColor.RED : NamedTextColor.GREEN)));
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    // ===== Kit =====

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> kitSubcommand() {
        return Commands.literal("kit")
                .requires(src -> src.getSender().hasPermission("purpurpvp.admin"))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) return Command.SINGLE_SUCCESS;
                                    String name = StringArgumentType.getString(ctx, "name");
                                    Kit kit = plugin.getKitManager().createFromInventory(player, name);
                                    plugin.getKitManager().saveGlobalKit(kit);
                                    player.sendMessage(Component.text("Global kit '" + name + "' created!", NamedTextColor.GREEN));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> { plugin.getKitManager().getAllGlobalKits().forEach(k -> b.suggest(k.getName())); return b.buildFuture(); })
                                .executes(ctx -> {
                                    plugin.getKitManager().deleteGlobalKit(StringArgumentType.getString(ctx, "name"));
                                    ctx.getSource().getSender().sendMessage(Component.text("Deleted!", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            Collection<Kit> kits = plugin.getKitManager().getAllGlobalKits();
                            if (kits.isEmpty()) { ctx.getSource().getSender().sendMessage(Component.text("No global kits.", NamedTextColor.YELLOW)); return Command.SINGLE_SUCCESS; }
                            ctx.getSource().getSender().sendMessage(Component.text("--- Global Kits ---", NamedTextColor.GOLD));
                            for (Kit k : kits) {
                                ctx.getSource().getSender().sendMessage(Component.text("  " + k.getName(), NamedTextColor.WHITE));
                            }
                            return Command.SINGLE_SUCCESS;
                        }));
    }

    // ===== Helpers =====

    private void showStats(Player viewer, Player target) {
        PlayerProfile profile = plugin.getRankManager().getProfile(target.getUniqueId());
        if (profile == null) { viewer.sendMessage(Component.text("No data.", NamedTextColor.RED)); return; }
        viewer.sendMessage(Component.text("--- " + profile.getUsername() + " ---", NamedTextColor.GOLD));
        viewer.sendMessage(Component.text("  Level: " + profile.getLevel() + " (" + profile.getXp() + " XP)", NamedTextColor.WHITE));
        viewer.sendMessage(Component.text("  Elo: " + String.format("%.0f", profile.getEloRating()), NamedTextColor.YELLOW));
        viewer.sendMessage(Component.text("  W/L: " + profile.getWins() + "/" + profile.getLosses(), NamedTextColor.GREEN));
        viewer.sendMessage(Component.text("  K/D: " + profile.getKills() + "/" + profile.getDeaths(), NamedTextColor.AQUA));
    }

    private void sendHelp(CommandSourceStack source) {
        source.getSender().sendMessage(Component.text("--- PurpurPvP ---", NamedTextColor.DARK_PURPLE));
        source.getSender().sendMessage(Component.text("  /duel <player> [kit]", NamedTextColor.GOLD));
        source.getSender().sendMessage(Component.text("  /queue <mode>", NamedTextColor.GOLD));
        source.getSender().sendMessage(Component.text("  /purpurpvp stats [player]", NamedTextColor.GOLD));
        source.getSender().sendMessage(Component.text("  /purpurpvp leaderboard", NamedTextColor.GOLD));
        if (source.getSender().hasPermission("purpurpvp.admin")) {
            source.getSender().sendMessage(Component.text("  /purpurpvp reload | arena | kit | setlobby", NamedTextColor.GOLD));
        }
    }
}

