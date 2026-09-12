package com.usainsrht.purpurpvp.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.usainsrht.purpurpvp.PurpurPvP;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

/**
 * /queue <mode> — Join matchmaking queue.
 * /queue leave  — Leave current queue.
 * Fully localized with YamlMessageAPI.
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
                        plugin.getMessageService().send(player, "error.invalid-args",
                                Placeholder.parsed("usage", "/queue <mode> | /queue leave (Modes: " + String.join(", ", plugin.getQueueManager().getAvailableModes()) + ")"));
                    }
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("leave")
                        .executes(ctx -> {
                            if (ctx.getSource().getSender() instanceof Player player) {
                                if (!plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                                    plugin.getMessageService().send(player, "queue.left");
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
                                    plugin.getMessageService().send(player, "queue.invalid-mode",
                                            Placeholder.parsed("modes", String.join(", ", plugin.getQueueManager().getAvailableModes())));
                                    return Command.SINGLE_SUCCESS;
                                }
                                if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                                    plugin.getMessageService().send(player, "queue.already-in-queue");
                                    return Command.SINGLE_SUCCESS;
                                }
                                plugin.getQueueManager().joinQueue(player.getUniqueId(), mode);
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
    }
}
