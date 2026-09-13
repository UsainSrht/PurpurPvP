package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.ffa.FfaArena;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.Collection;

/**
 * /ffa [arena] — Join a Free-For-All arena.
 * /ffa leave   — Return to the spawn lobby.
 * Fully localized with CommandLocalizationManager.
 */
@SuppressWarnings("UnstableApiUsage")
public class FfaCommand {

    private final PurpurPvP plugin;

    public FfaCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        var clm = plugin.getCommandLocalizationManager();
        String cmdName = clm.getName("ffa", "ffa");
        String leaveSub = clm.getSubcommand("ffa", "leave", "leave");
        String listSub = clm.getSubcommand("ffa", "list", "list");

        var root = Commands.literal(cmdName)
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        if (plugin.getFfaManager().isInFfa(player.getUniqueId())) {
                            plugin.getMessageService().send(player, "ffa.already-in-ffa");
                        } else {
                            plugin.getFfaManager().joinFfa(player, "classic");
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                });

        // /ffa leave
        root.then(Commands.literal(leaveSub)
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        if (plugin.getFfaManager().isInFfa(player.getUniqueId())) {
                            plugin.getFfaManager().leaveFfa(player);
                        } else {
                            plugin.getMessageService().send(player, "ffa.not-in-ffa");
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }));
        if (!leaveSub.equalsIgnoreCase("leave")) {
            root.then(Commands.literal("leave")
                    .executes(ctx -> {
                        if (ctx.getSource().getSender() instanceof Player player) {
                            if (plugin.getFfaManager().isInFfa(player.getUniqueId())) {
                                plugin.getFfaManager().leaveFfa(player);
                            } else {
                                plugin.getMessageService().send(player, "ffa.not-in-ffa");
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    }));
        }

        // /ffa list
        root.then(Commands.literal(listSub)
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        Collection<FfaArena> arenas = plugin.getFfaManager().getAllArenas();
                        for (FfaArena a : arenas) {
                            plugin.getMessageService().sendWithoutPrefix(player, "ffa.list-entry",
                                    Placeholder.parsed("arena", a.getName()),
                                    Placeholder.parsed("players", String.valueOf(a.getPlayers().size())));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }));

        // /ffa <arena>
        root.then(Commands.argument("arena", StringArgumentType.word())
                .suggests((ctx, b) -> {
                    for (FfaArena a : plugin.getFfaManager().getAllArenas()) b.suggest(a.getName());
                    return b.buildFuture();
                })
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        String arenaName = StringArgumentType.getString(ctx, "arena");
                        plugin.getFfaManager().joinFfa(player, arenaName);
                    }
                    return Command.SINGLE_SUCCESS;
                }));

        return root.build();
    }
}
