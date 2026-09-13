package com.usainsrht.purpurpvp.command;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages customizable command names, aliases, descriptions, and subcommand names from commands.yml.
 * Guarantees 100% localization of all command inputs.
 */
public class CommandLocalizationManager {

    private final JavaPlugin plugin;
    private final Logger logger;
    private File file;
    private FileConfiguration config;

    public CommandLocalizationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(plugin.getDataFolder(), "commands.yml");
        if (!file.exists()) {
            try (InputStream in = plugin.getResource("commands.yml")) {
                if (in != null) {
                    Files.copy(in, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                logger.warning("[CommandLocalization] Could not create commands.yml: " + e.getMessage());
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getName(String commandKey, String defaultName) {
        return config.getString("commands." + commandKey + ".name", defaultName);
    }

    public List<String> getAliases(String commandKey) {
        List<String> list = config.getStringList("commands." + commandKey + ".aliases");
        return list != null ? list : Collections.emptyList();
    }

    public String getDescription(String commandKey, String defaultDescription) {
        return config.getString("commands." + commandKey + ".description", defaultDescription);
    }

    public String getSubcommand(String commandKey, String subKey, String defaultSubcommand) {
        return config.getString("commands." + commandKey + ".subcommands." + subKey, defaultSubcommand);
    }

    /**
     * Checks if a provided input matches the configured subcommand or its default English fallback.
     */
    public boolean matchesSubcommand(String commandKey, String subKey, String defaultSubcommand, String input) {
        String configured = getSubcommand(commandKey, subKey, defaultSubcommand);
        return input.equalsIgnoreCase(configured) || input.equalsIgnoreCase(defaultSubcommand);
    }
}
