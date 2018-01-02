package com.rexcantor64.multilanguageplugin;

import com.google.common.io.ByteStreams;
import com.rexcantor64.multilanguageplugin.components.api.ChatColor;
import com.rexcantor64.multilanguageplugin.config.LanguageConfig;
import com.rexcantor64.multilanguageplugin.config.MainConfig;
import com.rexcantor64.multilanguageplugin.config.interfaces.Configuration;
import com.rexcantor64.multilanguageplugin.config.interfaces.ConfigurationProvider;
import com.rexcantor64.multilanguageplugin.config.interfaces.YamlConfiguration;
import com.rexcantor64.multilanguageplugin.guiapi.GuiManager;
import com.rexcantor64.multilanguageplugin.language.LanguageManager;
import com.rexcantor64.multilanguageplugin.language.LanguageParser;
import com.rexcantor64.multilanguageplugin.migration.LanguageMigration;
import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.multilanguageplugin.player.PlayerManager;
import com.rexcantor64.multilanguageplugin.plugin.PluginLoader;
import com.rexcantor64.multilanguageplugin.web.GistManager;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public abstract class MultiLanguagePlugin {

    // Main instances
    static MultiLanguagePlugin instance;
    PluginLoader loader;

    // File-related variables
    private File languageFolder;

    // Configs
    private Configuration configYAML;
    MainConfig config;
    private LanguageConfig languageConfig;
    private Configuration messagesConfig;

    // Managers
    private LanguageManager languageManager;
    private LanguageParser languageParser;
    private PlayerManager playerManager;
    GuiManager guiManager;
    private GistManager gistManager;

    public void reload() {
        configYAML = loadYAML("config");
        config.setup();
        messagesConfig = loadYAML("message");
        languageConfig.setup();
        languageManager.setup();
    }

    void onEnable() {
        languageFolder = new File(getDataFolder(), "languages");
        // Setup config.yml
        configYAML = loadYAML("config");
        (config = new MainConfig(this)).setup();
        // Setup messages.yml
        messagesConfig = loadYAML("message");
        // Start migration. Remove on v1.1.0.
        LanguageMigration.migrate();
        // Setup more classes
        (languageConfig = new LanguageConfig()).setup();
        (languageManager = new LanguageManager()).setup();
        playerManager = new PlayerManager();
        languageParser = new LanguageParser();
        guiManager = new GuiManager();
        gistManager = new GistManager(this);
    }

    public Configuration loadYAML(String fileName) {
        File f = getResource(fileName + ".yml");
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(f);
        } catch (Exception e) {
            logError("Failed to load %1.yml: %2", fileName, e.getMessage());
            logError("You'll likely receive more errors on console until the next restart.");
        }
        return null;
    }

    public MainConfig getConf() {
        return config;
    }

    public LanguageConfig getLanguageConfig() {
        return languageConfig;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public LanguageParser getLanguageParser() {
        return languageParser;
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public GistManager getGistManager() {
        return gistManager;
    }

    public abstract ProtocolLibListener getProtocolLibListener();

    public String getMessage(String code, String def, Object... args) {
        String s = ChatColor.translateAlternateColorCodes('&',
                messagesConfig.getString(code, def));
        for (int i = 0; i < args.length; i++)
            if (args[i] != null)
                s = s.replace("%" + (i + 1), args[i].toString());
        return s;
    }

    public List<String> getMessageList(String code, String... def) {
        List<String> result = messagesConfig.getStringList(code);
        if (result.size() == 0)
            result = Arrays.asList(def);
        return result;
    }

    public File getLanguageFolder() {
        if (!languageFolder.exists())
            try {
                if (!languageFolder.mkdirs())
                    logWarning("Failed to create folder 'languages'! Please check the folder permissions or create it manually!");
            } catch (Exception e) {
                logError("Failed to create folder 'languages'! Please check the folder permissions or create it manually! Error: %1", e.getMessage());
            }
        return languageFolder;
    }

    public void logInfo(String info, Object... arguments) {
        if (info == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                info = info.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        loader.getLogger().log(Level.INFO, info);
    }

    public void logWarning(String warning, Object... arguments) {
        if (warning == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                warning = warning.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        loader.getLogger().log(Level.WARNING, warning);
    }

    public void logError(String error, Object... arguments) {
        if (error == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                error = error.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        loader.getLogger().log(Level.SEVERE, error);
    }

    public void logDebug(String info, Object... arguments) {
        if (info == null) return;
        if (!config.isDebug()) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                info = info.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        loader.getLogger().log(Level.INFO, "[DEBUG] " + info);
    }

    public void logDebugWarning(String warning, Object... arguments) {
        if (!config.isDebug()) return;
        if (warning == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                warning = warning.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        loader.getLogger().log(Level.WARNING, "[DEBUG] " + warning);
    }

    public abstract File getDataFolder();

    public Configuration getConfig() {
        return configYAML;
    }

    public File getResource(String fileName) {
        File folder = getDataFolder();
        if (!folder.exists())
            if (!folder.mkdirs())
                logError("Failed to create plugin folder!");
        File resourceFile = new File(folder, fileName);
        try {
            if (!resourceFile.exists()) {
                if (!resourceFile.createNewFile())
                    logError("Failed to create the file %1!", fileName);
                try (InputStream in = loader.getResourceAsStream(fileName);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configYAML, new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            logError("Failed to save config.yml! Cause: %1", e.getMessage());
        }
    }

    public PluginLoader getLoader() {
        return loader;
    }

    public static MultiLanguagePlugin get() {
        return instance;
    }

}
