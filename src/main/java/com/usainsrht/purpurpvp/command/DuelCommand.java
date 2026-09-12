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
import me.usainsrht.itemapi.itemtext.ItemText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * /duel <player> [kit] — Challenge a player or open room browser.
 * /duel accept / decline — Respond to pending requests.
 * Fully localized with YamlMessageAPI and ItemAPI.
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
        if (target == null) {
            plugin.getMessageService().send(player, "error.player-not-found");
            return;
        }
        if (target.equals(player)) {
            plugin.getMessageService().send(player, "duel.cannot-duel-self");
            return;
        }
        if (plugin.getMatchManager().isInMatch(player.getUniqueId())) {
            plugin.getMessageService().send(player, "duel.already-in-match");
            return;
        }
        if (plugin.getMatchManager().isInMatch(target.getUniqueId())) {
            plugin.getMessageService().send(player, "duel.target-in-match", Placeholder.parsed("player", target.getName()));
            return;
        }

        DuelRequest request = new DuelRequest(player.getUniqueId(), target.getUniqueId(), kit);
        pendingRequests.put(target.getUniqueId(), request);

        ItemStack icon = kit != null && kit.getIcon() != null ? kit.getIcon() : new ItemStack(Material.IRON_SWORD);
        Component kitComp = ItemText.format(icon);
        String kitName = kit != null ? kit.getName() : "Selected by Opponent";

        plugin.getMessageService().send(player, "duel.request-sent",
                Placeholder.parsed("player", target.getName()),
                Placeholder.component("kit", kitComp));

        plugin.getMessageService().send(target, "duel.request-received",
                Placeholder.parsed("player", player.getName()),
                Placeholder.component("kit", kitComp),
                Placeholder.parsed("arena", "Dynamic Arena"));

        // Expire after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DuelRequest pending = pendingRequests.get(target.getUniqueId());
            if (pending != null && pending.equals(request)) {
                pendingRequests.remove(target.getUniqueId());
                Player s = Bukkit.getPlayer(request.sender);
                if (s != null) plugin.getMessageService().send(s, "duel.request-expired", Placeholder.parsed("player", target.getName()));
            }
        }, 600L);
    }

    private void acceptDuel(Player player) {
        DuelRequest request = pendingRequests.remove(player.getUniqueId());
        if (request == null) {
            plugin.getMessageService().send(player, "duel.request-expired", Placeholder.parsed("player", "Duel"));
            return;
        }
        Player challenger = Bukkit.getPlayer(request.sender);
        if (challenger == null) {
            plugin.getMessageService().send(player, "error.player-not-found");
            return;
        }

        if (request.kit != null) {
            startDuel(challenger, player, request.kit);
        } else {
            plugin.getKitSelectorGUI().open(player, kit -> startDuel(challenger, player, kit));
        }
    }

    private void declineDuel(Player player) {
        DuelRequest request = pendingRequests.remove(player.getUniqueId());
        if (request != null) {
            plugin.getMessageService().send(player, "duel.declined");
            Player challenger = Bukkit.getPlayer(request.sender);
            if (challenger != null) {
                plugin.getMessageService().send(challenger, "duel.declined");
            }
        }
    }

    private void startDuel(Player p1, Player p2, Kit kit) {
        plugin.getMessageService().send(p1, "duel.accepted");
        plugin.getMessageService().send(p2, "duel.accepted");

        MatchConfig config = new MatchConfig();
        config.setKit(kit);
        config.setTeamSize(1);
        config.setTeamCount(2);

        List<Team> teams = TeamAllocator.allocateRandom(List.of(p1.getUniqueId(), p2.getUniqueId()), 2, 1);
        plugin.getMatchManager().createMatch(config, teams);
    }

    private record DuelRequest(UUID sender, UUID target, Kit kit) {}
}
