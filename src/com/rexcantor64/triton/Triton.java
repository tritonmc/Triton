package com.rexcantor64.triton;

import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.config.LanguageConfig;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.migration.LanguageMigration;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.MysqlStorage;
import com.rexcantor64.triton.storage.PlayerStorage;
import com.rexcantor64.triton.storage.YamlStorage;
import com.rexcantor64.triton.web.TwinManager;
import net.md_5.bungee.api.ChatColor;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public abstract class Triton implements com.rexcantor64.triton.api.Triton {

    // Main instances
    static Triton instance;
    PluginLoader loader;
    GuiManager guiManager;
    // File-related variables
    private File translationsFolder;
    // Configs
    private Configuration configYAML;
    private MainConfig config;
    private LanguageConfig languageConfig;
    private Configuration messagesConfig;
    // Managers
    private LanguageManager languageManager;
    private LanguageParser languageParser;
    private TwinManager twinManager;
    private PlayerManager playerManager;
    private PlayerStorage playerStorage;

    public static boolean isBungee() {
        return instance instanceof BungeeMLP;
    }

    public static Triton get() {
        return instance;
    }

    public static SpigotMLP asSpigot() {
        return (SpigotMLP) instance;
    }

    public static BungeeMLP asBungee() {
        return (BungeeMLP) instance;
    }

    public void reload() {
        configYAML = loadYAML("config", isBungee() ? "bungee_config" : "config");
        config.setup();
        setupStorage();
        messagesConfig = loadYAML("messages", "messages");
        languageConfig.setup(config.isBungeecord());
        languageManager.setup();
        for (LanguagePlayer lp : playerManager.getAll())
            lp.refreshAll();
        startConfigRefreshTask();
    }

    public Configuration loadYAML(String fileName, String internalFileName) {
        File f = getResource(fileName + ".yml", internalFileName + ".yml");
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

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public TwinManager getTwinManager() {
        return twinManager;
    }

    public PlayerStorage getPlayerStorage() {
        return playerStorage;
    }

    public abstract String getVersion();

    public abstract ProtocolLibListener getProtocolLibListener();

    protected abstract void startConfigRefreshTask();

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

    public File getTranslationsFolder() {
        return translationsFolder;
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

    public File getResource(String fileName, String internalFileName) {
        File folder = getDataFolder();
        if (!folder.exists())
            if (!folder.mkdirs())
                logError("Failed to create plugin folder!");
        File resourceFile = new File(folder, fileName);
        try {
            if (!resourceFile.exists()) {
                if (!resourceFile.createNewFile())
                    logError("Failed to create the file %1!", fileName);
                try (InputStream in = loader.getResourceAsStream(internalFileName);
                     OutputStream out = new FileOutputStream(resourceFile)) {
                    ByteStreams.copy(in, out);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resourceFile;
    }

    void onEnable() {
        translationsFolder = new File(getDataFolder(), "translations");
        // Setup config.yml
        configYAML = loadYAML("config", isBungee() ? "bungee_config" : "config");
        (config = new MainConfig(this)).setup();
        // Setup messages.yml
        messagesConfig = loadYAML("messages", "messages");
        // Start migration from v1 to v2.
        LanguageMigration.migrate();
        // Setup more classes
        setupStorage();
        (languageConfig = new LanguageConfig()).setup(config.isBungeecord());
        (languageManager = new LanguageManager()).setup();
        languageParser = new LanguageParser();
        playerManager = new PlayerManager();
        twinManager = new TwinManager(this);
    }

    private void setupStorage() {
        if (config.isMysql()) {
            MysqlStorage mysqlStorage = new MysqlStorage(config.getMysqlHost(), config.getMysqlPort(), config
                    .getMysqlDatabase(), config.getMysqlUser(), config.getMysqlPassword(), config
                    .getMysqlTablePrefix());
            this.playerStorage = mysqlStorage;
            if (mysqlStorage.setup()) return;
            logError("Failed to connect to database, falling back to YAML storage!");

        }
        this.playerStorage = new YamlStorage();
    }

    public void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(configYAML, new File(getDataFolder(),
                    "config.yml"));
        } catch (IOException e) {
            logError("Failed to save config.yml! Cause: %1", e.getMessage());
        }
    }

    public SpigotBridgeManager getBridgeManager() {
        return null;
    }

    public PluginLoader getLoader() {
        return loader;
    }

    public void openLanguagesSelectionGUI(com.rexcantor64.triton.api.players.LanguagePlayer p) {
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

}
