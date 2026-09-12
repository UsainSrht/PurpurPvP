package com.usainsrht.purpurpvp.message;

import com.usainsrht.purpurpvp.PurpurPvP;
import me.usainsrht.yamlmessage.YamlMessage;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central service for retrieving and dispatching YamlMessages parsed with YamlMessageAPI.
 * Guarantees zero hardcoded text in code and supports dynamic MiniMessage placeholders.
 */
public class MessageService {

    private final PurpurPvP plugin;
    private final Logger logger;
    private final Map<String, YamlMessage> messageCache = new ConcurrentHashMap<>();
    private String prefix = "";

    public MessageService(PurpurPvP plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        messageCache.clear();
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(messagesFile);
        this.prefix = config.getString("prefix", "");

        loadSection("", config);
        logger.info("[MessageService] Loaded " + messageCache.size() + " messages via YamlMessageAPI.");
    }

    private void loadSection(String path, ConfigurationSection section) {
        for (String key : section.getKeys(false)) {
            String fullKey = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                ConfigurationSection sub = section.getConfigurationSection(key);
                if (sub != null) {
                    // Check if this section itself is a YamlMessage payload (contains chat, title, sound, or actionbar)
                    if (sub.contains("chat") || sub.contains("chats") || sub.contains("title")
                            || sub.contains("sound") || sub.contains("sounds")
                            || sub.contains("actionbar") || sub.contains("action-bar")) {
                        messageCache.put(fullKey, YamlMessage.parse(sub));
                    }
                    loadSection(fullKey, sub);
                }
            } else {
                Object raw = section.get(key);
                if (raw != null) {
                    messageCache.put(fullKey, YamlMessage.parse(raw));
                }
            }
        }
    }

    @NotNull
    public YamlMessage get(@NotNull String key) {
        YamlMessage msg = messageCache.get(key);
        if (msg == null) {
            return YamlMessage.chat("<red>Missing message: " + key + "</red>");
        }
        return msg;
    }

    public void send(@NotNull Audience audience, @NotNull String key, @NotNull TagResolver... resolvers) {
        YamlMessage msg = get(key);
        if (prefix != null && !prefix.isEmpty()) {
            msg.send(audience, prefix, resolvers);
        } else {
            msg.send(audience, resolvers);
        }
    }

    public void sendWithoutPrefix(@NotNull Audience audience, @NotNull String key, @NotNull TagResolver... resolvers) {
        get(key).send(audience, resolvers);
    }

    public void send(@NotNull Player player, @NotNull String key, @NotNull TagResolver... resolvers) {
        send((Audience) player, key, resolvers);
    }

    public void sendWithoutPrefix(@NotNull Player player, @NotNull String key, @NotNull TagResolver... resolvers) {
        sendWithoutPrefix((Audience) player, key, resolvers);
    }

    public void broadcast(@NotNull String key, @NotNull TagResolver... resolvers) {
        send(plugin.getServer(), key, resolvers);
    }

    @NotNull
    public Component toComponent(@NotNull String key, @NotNull TagResolver... resolvers) {
        YamlMessage msg = get(key);
        Component chat = msg.firstChatComponent(resolvers);
        if (!chat.equals(Component.empty())) return chat;
        Component ab = msg.actionbarComponent(resolvers);
        if (!ab.equals(Component.empty())) return ab;
        if (msg.title() != null) {
            try {
                return msg.title().toTitle(resolvers).title();
            } catch (Exception ignored) { }
        }
        return Component.empty();
    }

    @Nullable
    public String getPrefix() {
        return prefix;
    }
}
