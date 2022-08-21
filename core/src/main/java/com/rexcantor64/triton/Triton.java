package com.rexcantor64.triton;

import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.config.MessagesConfig;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.guiapi.GuiManager;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.migration.LanguageMigration;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.storage.MysqlStorage;
import com.rexcantor64.triton.storage.Storage;
import com.rexcantor64.triton.utils.FileUtils;
import com.rexcantor64.triton.web.TwinManager;
import lombok.Getter;
import lombok.val;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

@Getter
public abstract class Triton<P extends LanguagePlayer, B extends BridgeManager> implements com.rexcantor64.triton.api.Triton {

    // Main instances
    protected static Triton<?, ?> instance;
    protected PluginLoader loader;
    GuiManager guiManager;
    // File-related variables
    private File translationsFolder;
    // Configs
    private Configuration configYAML;
    private MainConfig config;
    private MessagesConfig messagesConfig;
    // Managers
    private LanguageManager languageManager;
    private LanguageParser languageParser;
    private TwinManager twinManager;
    protected final PlayerManager<P> playerManager;
    protected final B bridgeManager;
    private Storage storage;
    private TritonLogger logger;

    protected Triton(PlayerManager<P> playerManager, B bridgeManager) {
        this.playerManager = playerManager;
        this.bridgeManager = bridgeManager;
    }

    public static Platform type() {
        return instance.getLoader().getPlatform();
    }

    public static boolean isBungee() {
        return type() == Platform.BUNGEE;
    }

    public static boolean isVelocity() {
        return type() == Platform.VELOCITY;
    }

    public static boolean isProxy() {
        return type().isProxy();
    }

    public static boolean isSpigot() {
        return type() == Platform.SPIGOT;
    }

    public static Triton<?, ?> get() {
        return instance;
    }

    public static SpigotMLP asSpigot() {
        return (SpigotMLP) instance;
    }

    public static BungeeMLP asBungee() {
        return (BungeeMLP) instance;
    }

    protected void onEnable() {
        translationsFolder = new File(getDataFolder(), "translations");

        logger = loader.getTritonLogger();

        config = new MainConfig(this);
        languageManager = new LanguageManager();
        messagesConfig = new MessagesConfig();

        LanguageMigration.migrate();

        reload();

        languageParser = new LanguageParser();
        twinManager = new TwinManager(this);
    }

    public void reload() {
        configYAML = loadYAML("config", isProxy() ? "bungee_config" : "config");
        config.setup();
        logger.setLogLevel(config.getLogLevel());
        messagesConfig.setup();
        setupStorage();
        languageManager.setup();
        startConfigRefreshTask();
    }

    public void refreshPlayers() {
        for (LanguagePlayer lp : playerManager.getAll())
            lp.refreshAll();
    }

    public Configuration loadYAML(String fileName, String internalFileName) {
        File f = FileUtils.getResource(fileName + ".yml", internalFileName + ".yml");
        try {
            val stream = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8);
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(stream);
        } catch (Exception e) {
            logger.logError(e, "Failed to load %1.yml.", fileName);
            logger.logError("You'll likely receive more errors on console until the next restart.");
        }
        return null;
    }

    public abstract String getVersion();

    protected abstract void startConfigRefreshTask();

    public abstract void runAsync(Runnable runnable);

    public abstract File getDataFolder();

    private void setupStorage() {
        if (config.getStorageType().equalsIgnoreCase("mysql")) {
            try {
                val mysqlStorage = new MysqlStorage(config.getDatabaseHost(), config.getDatabasePort(), config
                        .getDatabaseName(), config.getDatabaseUser(), config.getDatabasePassword(), config
                        .getDatabaseTablePrefix());
                this.storage = mysqlStorage;
                mysqlStorage.load();
                logger.logInfo("Loaded MySQL storage manager");
                return;
            } catch (Exception e) {
                logger.logError(e, "Failed to connect to database, falling back to local storage!");
            }
        }
        this.storage = new LocalStorage();
        this.storage.load();
        logger.logInfo("Loaded local storage manager");
    }

    public void openLanguagesSelectionGUI(com.rexcantor64.triton.api.players.LanguagePlayer p) {
    }

    public abstract UUID getPlayerUUIDFromString(String input);

    public abstract short getMcVersion();

    public abstract short getMinorMcVersion();

}
