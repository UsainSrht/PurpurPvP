package com.usainsrht.purpurpvp.chat;

import com.usainsrht.purpurpvp.PurpurPvP;
import com.usainsrht.purpurpvp.match.Match;
import com.usainsrht.purpurpvp.ranked.PlayerProfile;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/**
 * Contextual chat system that routes messages based on the player's current state.
 * - Lobby players only see lobby chat
 * - Match players only see match chat
 * - Spectators can read match chat but participants can't see spectator chat
 */
public class ChatManager implements Listener {

    private final PurpurPvP plugin;

    public ChatManager(PurpurPvP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        ChatScope scope = getScope(sender);

        // Cancel default broadcast — we handle routing ourselves
        event.setCancelled(true);

        String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component formatted = formatMessage(sender, plainMessage, scope);

        switch (scope) {
            case MATCH -> broadcastToMatch(sender, formatted);
            case SPECTATOR -> broadcastToSpectators(sender, formatted);
            case LOBBY -> broadcastToLobby(formatted);
            case GLOBAL -> broadcastToLobby(formatted);
        }
    }

    /**
     * Determine the chat scope of a player.
     */
    public ChatScope getScope(Player player) {
        Match match = plugin.getMatchManager().getMatchOfPlayer(player.getUniqueId());
        if (match != null) {
            if (match.isSpectator(player.getUniqueId())) {
                return ChatScope.SPECTATOR;
            }
            return ChatScope.MATCH;
        }
        return ChatScope.LOBBY;
    }

    private Component formatMessage(Player sender, String message, ChatScope scope) {
        PlayerProfile profile = plugin.getRankManager().getProfile(sender.getUniqueId());
        String levelPrefix = profile != null ? "[Lv." + profile.getLevel() + "] " : "";

        return switch (scope) {
            case LOBBY -> Component.text("[Lobby] ", NamedTextColor.GRAY)
                    .append(Component.text(levelPrefix, NamedTextColor.GOLD))
                    .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                    .append(Component.text(": " + message, NamedTextColor.WHITE));
            case MATCH -> Component.text("[Match] ", NamedTextColor.AQUA)
                    .append(Component.text(levelPrefix, NamedTextColor.GOLD))
                    .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                    .append(Component.text(": " + message, NamedTextColor.WHITE));
            case SPECTATOR -> Component.text("[Spectator] ", NamedTextColor.GRAY)
                    .append(Component.text(sender.getName(), NamedTextColor.GRAY))
                    .append(Component.text(": " + message, NamedTextColor.GRAY));
            case GLOBAL -> Component.text(levelPrefix, NamedTextColor.GOLD)
                    .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                    .append(Component.text(": " + message, NamedTextColor.WHITE));
        };
    }

    /**
     * Broadcast to participants + spectators of the same match.
     */
    private void broadcastToMatch(Player sender, Component message) {
        Match match = plugin.getMatchManager().getMatchOfPlayer(sender.getUniqueId());
        if (match == null) return;

        // Send to all participants
        for (UUID uuid : match.getAllPlayerUUIDs()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
        // Also send to spectators
        for (UUID uuid : match.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    /**
     * Broadcast spectator chat only to other spectators (not to match participants).
     */
    private void broadcastToSpectators(Player sender, Component message) {
        Match match = plugin.getMatchManager().getMatchOfPlayer(sender.getUniqueId());
        if (match == null) return;

        for (UUID uuid : match.getSpectators()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(message);
        }
    }

    /**
     * Broadcast to all players NOT in a match.
     */
    private void broadcastToLobby(Component message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!plugin.getMatchManager().isInMatch(p.getUniqueId())) {
                p.sendMessage(message);
            }
        }
    }
}


