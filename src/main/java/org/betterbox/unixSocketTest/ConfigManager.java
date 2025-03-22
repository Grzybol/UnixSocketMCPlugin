package org.betterbox.unixSocketTest;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadConfig();
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        verifyConfig();
    }

    private void verifyConfig() {
        if (getConfig().getString("socketName") == null) {
            getConfig().set("socketName", "/tmp/plugin_minecraft.sock");
        }
        if (getConfig().getString("authToken") == null) {
            getConfig().set("authToken", "your_default_token_here");
        }
        saveConfig();
    }

    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        verifyConfig();
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config to " + configFile + ": " + e.getMessage());
        }
    }

    public String getSocketName() {
        return getConfig().getString("socketName");
    }

    public String getAuthToken() {
        return getConfig().getString("authToken");
    }
}
