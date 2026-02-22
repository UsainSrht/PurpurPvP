package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

/**
 * /queue <mode> — Join matchmaking queue.
 * /queue leave  — Leave current queue.
 */
@SuppressWarnings("UnstableApiUsage")
public class QueueCommand {

    private final PurpurPvP plugin;

    public QueueCommand(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    public LiteralCommandNode<CommandSourceStack> buildNode() {
        return Commands.literal("queue")
                .requires(src -> src.getSender().hasPermission("purpurpvp.queue"))
                .executes(ctx -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        player.sendMessage(Component.text("Usage: /queue <mode> | /queue leave", NamedTextColor.YELLOW));
                        player.sendMessage(Component.text("Modes: " + String.join(", ", plugin.getQueueManager().getAvailableModes()), NamedTextColor.GRAY));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                if (!plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                                    player.sendMessage(Component.text("Not in a queue!", NamedTextColor.RED));
                                } else {
                                    plugin.getQueueManager().leaveQueue(player.getUniqueId());
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            for (String mode : plugin.getQueueManager().getAvailableModes()) builder.suggest(mode);
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                String mode = StringArgumentType.getString(ctx, "mode");
                                if (!plugin.getQueueManager().getAvailableModes().contains(mode)) {
                                    player.sendMessage(Component.text("Invalid mode!", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                                    player.sendMessage(Component.text("Already in queue! /queue leave first.", NamedTextColor.RED));
                                    return Command.SINGLE_SUCCESS;
                                }
                                plugin.getQueueManager().joinQueue(player.getUniqueId(), mode);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}

