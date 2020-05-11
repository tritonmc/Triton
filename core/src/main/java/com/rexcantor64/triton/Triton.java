package com.rexcantor64.triton;

import com.rexcantor64.triton.bridge.SpigotBridgeManager;
import com.rexcantor64.triton.config.LanguageConfig;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.config.MessagesConfig;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.logger.Logger;
import com.rexcantor64.triton.migration.LanguageMigration;
import com.rexcantor64.triton.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.storage.MysqlStorage;
import com.rexcantor64.triton.storage.Storage;
import com.rexcantor64.triton.utils.FileUtils;
import com.rexcantor64.triton.web.TwinManager;
import lombok.Getter;

import java.io.File;

@Getter
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
    private MessagesConfig messagesConfig;
    // Managers
    private LanguageManager languageManager;
    private LanguageParser languageParser;
    private TwinManager twinManager;
    private PlayerManager playerManager;
    private Storage storage;
    private Logger logger;

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

    void onEnable() {
        translationsFolder = new File(getDataFolder(), "translations");

        logger = new Logger(loader.getLogger());

        config = new MainConfig(this);
        languageConfig = new LanguageConfig();
        languageManager = new LanguageManager();
        playerManager = new PlayerManager();
        messagesConfig = new MessagesConfig();
        reload();

        LanguageMigration.migrate();

        languageParser = new LanguageParser();
        twinManager = new TwinManager(this);
    }

    public void reload() {
        configYAML = loadYAML("config", isBungee() ? "bungee_config" : "config");
        config.setup();
        logger.setLogLevel(config.getLogLevel());
        setupStorage();
        messagesConfig.setup();
        languageConfig.setup(config.isBungeecord());
        languageManager.setup();
        for (LanguagePlayer lp : playerManager.getAll())
            lp.refreshAll();
        startConfigRefreshTask();
    }

    public Configuration loadYAML(String fileName, String internalFileName) {
        File f = FileUtils.getResource(fileName + ".yml", internalFileName + ".yml");
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(f);
        } catch (Exception e) {
            logger.logError("Failed to load %1.yml: %2", fileName, e.getMessage());
            logger.logError("You'll likely receive more errors on console until the next restart.");
        }
        return null;
    }

    public MainConfig getConf() {
        return config;
    }

    public abstract String getVersion();

    public abstract ProtocolLibListener getProtocolLibListener();

    protected abstract void startConfigRefreshTask();

    public abstract void runAsync(Runnable runnable);

    public abstract File getDataFolder();

    private void setupStorage() {
        if (config.isMysql()) {
            MysqlStorage mysqlStorage = new MysqlStorage(config.getMysqlHost(), config.getMysqlPort(), config
                    .getMysqlDatabase(), config.getMysqlUser(), config.getMysqlPassword(), config
                    .getMysqlTablePrefix());
            this.storage = mysqlStorage;
            if (mysqlStorage.setup()) return;
            logger.logError("Failed to connect to database, falling back to YAML storage!");

        }
        this.storage = new LocalStorage();
    }

    public SpigotBridgeManager getBridgeManager() {
        return null;
    }

    public void openLanguagesSelectionGUI(com.rexcantor64.triton.api.players.LanguagePlayer p) {
    }

}
