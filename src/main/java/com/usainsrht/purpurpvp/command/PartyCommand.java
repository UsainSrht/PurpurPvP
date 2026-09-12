package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.kit.Kit;
import com.usainsrht.purpurpvp.party.Party;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Modern Brigadier /party command for party management and party vs party duels.
 */
@SuppressWarnings("UnstableApiUsage")
public class PartyCommand {

    private final PurpurPvP plugin;

    public PartyCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("party")
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        if (plugin.getPartyManager().hasParty(player.getUniqueId())) {
                            plugin.getPartyManager().sendPartyInfo(player);
                        } else {
                            plugin.getMessageService().send(player, "party.not-in-party");
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("create")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                plugin.getPartyManager().createParty(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests((ctx, b) -> {
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        if (!p.getName().equals(ctx.getSource().getSender().getName())) b.suggest(p.getName());
                                    }
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String targetName = StringArgumentType.getString(ctx, "player");
                                        Player target = Bukkit.getPlayerExact(targetName);
                                        if (target != null) {
                                            plugin.getPartyManager().invitePlayer(player, target);
                                        } else {
                                            plugin.getMessageService().send(player, "error.player-not-found");
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("accept")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String leaderName = StringArgumentType.getString(ctx, "player");
                                        plugin.getPartyManager().acceptInvite(player, leaderName);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("decline")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String leaderName = StringArgumentType.getString(ctx, "player");
                                        plugin.getPartyManager().declineInvite(player, leaderName);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                plugin.getPartyManager().leaveParty(player);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("disband")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
                                if (party != null && party.isLeader(player.getUniqueId())) {
                                    plugin.getPartyManager().disbandParty(party);
                                } else {
                                    plugin.getMessageService().send(player, "party.not-leader");
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("kick")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String targetName = StringArgumentType.getString(ctx, "player");
                                        Player target = Bukkit.getPlayerExact(targetName);
                                        if (target != null) {
                                            plugin.getPartyManager().kickMember(player, target);
                                        } else {
                                            plugin.getMessageService().send(player, "error.player-not-found");
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("leader")
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String targetName = StringArgumentType.getString(ctx, "player");
                                        Player target = Bukkit.getPlayerExact(targetName);
                                        if (target != null) {
                                            plugin.getPartyManager().transferLeader(player, target);
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("chat")
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String msg = StringArgumentType.getString(ctx, "message");
                                        plugin.getPartyManager().partyChat(player, msg);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("split")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
                                if (party != null && party.isLeader(player.getUniqueId())) {
                                    plugin.getPartyManager().startPartySplit(party, null);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("ffa")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                Party party = plugin.getPartyManager().getPartyOf(player.getUniqueId());
                                if (party != null && party.isLeader(player.getUniqueId())) {
                                    plugin.getPartyManager().startPartyFFA(party, null);
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.literal("duel")
                        .then(Commands.literal("accept")
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        plugin.getPartyManager().acceptPartyDuel(player);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> {
                                    if (ctx.getSource().getSender() instanceof Player player) {
                                        String targetName = StringArgumentType.getString(ctx, "target");
                                        Player target = Bukkit.getPlayerExact(targetName);
                                        if (target != null) {
                                            plugin.getPartyManager().sendPartyDuel(player, target, null);
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("kit", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            if (ctx.getSource().getSender() instanceof Player player) {
                                                String targetName = StringArgumentType.getString(ctx, "target");
                                                Player target = Bukkit.getPlayerExact(targetName);
                                                String kitName = StringArgumentType.getString(ctx, "kit");
                                                Kit kit = plugin.getKitManager().getGlobalKit(kitName);
                                                if (target != null) {
                                                    plugin.getPartyManager().sendPartyDuel(player, target, kit);
                                                }
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                .build();
    }
}
