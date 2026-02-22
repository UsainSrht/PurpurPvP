package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.arena.Arena;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.match.MatchConfig;
import com.usainsrht.purpurpvp.match.team.Team;
import com.usainsrht.purpurpvp.match.team.TeamAllocator;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /duel <player> [kit] — Challenge a player or open room browser.
 * /duel accept / decline — Respond to pending requests.
 */
@SuppressWarnings("UnstableApiUsage")
public class DuelCommand {

    private final PurpurPvP plugin;
    private final Map<UUID, DuelRequest> pendingRequests = new HashMap<>();

    public DuelCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("duel")
                .requires(src -> src.getSender().hasPermission("purpurpvp.duel"))
                // /duel (no args) — open room browser
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        plugin.getRoomBrowserGUI().open(player);
                    }
                    return Command.SINGLE_SUCCESS;
                })
                // /duel accept
                .then(Commands.literal("accept")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) acceptDuel(player);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /duel decline
                .then(Commands.literal("decline")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) declineDuel(player);
                            return Command.SINGLE_SUCCESS;
                        }))
                // /duel <player> [kit]
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                if (!p.getName().equals(ctx.getSource().getSender().getName()))
                                    builder.suggest(p.getName());
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                String targetName = StringArgumentType.getString(ctx, "player");
                                sendDuelRequest(player, targetName, null);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("kit", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> {
                                    for (Kit k : plugin.getKitManager().getAllGlobalKits()) builder.suggest(k.getName());
                                    if (ctx.getSource().getSender() instanceof Player p) {
                                        for (Kit k : plugin.getKitManager().getPlayerKits(p.getUniqueId())) builder.suggest(k.getName());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String targetName = StringArgumentType.getString(ctx, "player");
                                        String kitName = StringArgumentType.getString(ctx, "kit");
                                        Kit kit = plugin.getKitManager().getGlobalKit(kitName);
                                        if (kit == null) kit = plugin.getKitManager().getPlayerKit(player.getUniqueId(), kitName);
                                        sendDuelRequest(player, targetName, kit);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .build();
    }

    private void sendDuelRequest(Player player, String targetName, Kit kit) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) { player.sendMessage(Component.text("Player not found!", NamedTextColor.RED)); return; }
        if (target.equals(player)) { player.sendMessage(Component.text("You can't duel yourself!", NamedTextColor.RED)); return; }
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) { player.sendMessage(Component.text("You are already in a match!", NamedTextColor.RED)); return; }
        if (plugin.getMatchManager().isInMatch(target.getUniqueId())) { player.sendMessage(Component.text(target.getName() + " is in a match!", NamedTextColor.RED)); return; }

        // kit may be null — will prompt on accept
        DuelRequest request = new DuelRequest(player.getUniqueId(), target.getUniqueId(), kit);
        pendingRequests.put(target.getUniqueId(), request);

        player.sendMessage(Component.text("Duel request sent to ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.YELLOW)));

        target.sendMessage(Component.text(player.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" wants to duel! ", NamedTextColor.GREEN))
                .append(Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel accept")))
                .append(Component.text(" "))
                .append(Component.text("[DECLINE]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/duel decline"))));

        // Expire
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest pending = pendingRequests.get(target.getUniqueId());
            if (pending != null && pending.equals(request)) {
                pendingRequests.remove(target.getUniqueId());
                Player s = Bukkit.getPlayer(request.sender);
                if (s != null) s.sendMessage(Component.text("Duel request expired.", NamedTextColor.RED));
            }
        }, 600L);
    }

    private void acceptDuel(Player player) {
        DuelRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) { player.sendMessage(Component.text("No pending duel!", NamedTextColor.RED)); return; }
        Player challenger = Bukkit.getPlayer(request.sender);
        if (challenger == null) { player.sendMessage(Component.text("Challenger offline!", NamedTextColor.RED)); return; }

        if (request.kit != null) {
            startDuel(challenger, player, request.kit);
        } else {
            plugin.getKitSelectorGUI().open(player, kit -> startDuel(challenger, player, kit));
        }
    }

    private void startDuel(Player p1, Player p2, Kit kit) {
        MatchConfig config = new MatchConfig();
        config.setKit(kit);
        config.setTeamSize(1);
        config.setTeamCount(2);

        Arena arena = plugin.getArenaManager().getAvailableArena(2);
        if (arena == null) {
            p1.sendMessage(Component.text("No arena available!", NamedTextColor.RED));
            p2.sendMessage(Component.text("No arena available!", NamedTextColor.RED));
            return;
        }
        config.setArena(arena);

        List<Team> teams = TeamAllocator.allocateDuel(p1.getUniqueId(), p2.getUniqueId());
        plugin.getMatchManager().createMatch(config, teams);
        p1.sendMessage(Component.text("Duel starting!", NamedTextColor.GREEN));
        p2.sendMessage(Component.text("Duel starting!", NamedTextColor.GREEN));
    }

    private void declineDuel(Player player) {
        DuelRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) { player.sendMessage(Component.text("No pending duel!", NamedTextColor.RED)); return; }
        player.sendMessage(Component.text("Duel declined.", NamedTextColor.RED));
        Player challenger = Bukkit.getPlayer(request.sender);
        if (challenger != null) challenger.sendMessage(Component.text(player.getName() + " declined.", NamedTextColor.RED));
    }

    private record DuelRequest(UUID sender, UUID receiver, Kit kit) {}
}

