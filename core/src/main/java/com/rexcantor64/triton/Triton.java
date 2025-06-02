package com.rexcantor64.triton;

import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.api.legacy.LegacyLanguageParser;
import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.config.MessagesConfig;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.debug.DumpManager;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.TranslationManager;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.language.parser.LegacyParser;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.migration.LanguageMigration;
import com.rexcantor64.triton.packetinterceptor.PacketEventsManager;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.storage.MysqlStorage;
import com.rexcantor64.triton.storage.Storage;
import com.rexcantor64.triton.utils.FileUtils;
import com.rexcantor64.triton.utils.TritonAPIUtils;
import com.rexcantor64.triton.web.TwinManager;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import java.util.UUID;

@Getter
public abstract class Triton<P extends TritonLanguagePlayer<?>, B extends BridgeManager> implements com.rexcantor64.triton.api.Triton {

    // Main instances
    protected static Triton<?, ?> instance;
    protected PluginLoader loader;
    // File-related variables
    private File translationsFolder;
    // Configs
    private Configuration configYAML;
    private MainConfig config;
    private MessagesConfig messagesConfig;
    // Managers
    private LanguageManager languageManager;
    @Deprecated
    private final LanguageParser languageParser = new LegacyLanguageParser();
    private TranslationManager translationManager;
    private MessageParser messageParser;
    private TwinManager twinManager;
    protected final PlayerManager<P> playerManager;
    protected final B bridgeManager;
    private Storage storage;
    private TritonLogger logger;
    private DumpManager dumpManager;
    protected @Nullable PacketEventsManager packetEventsManager;

    protected Triton(PlayerManager<P> playerManager, B bridgeManager) {
        this.playerManager = playerManager;
        this.bridgeManager = bridgeManager;
    }

    public static Platform platform() {
        return instance.getLoader().getPlatform();
    }

    public static boolean isBungee() {
        return platform() == Platform.BUNGEE;
    }

    public static boolean isVelocity() {
        return platform() == Platform.VELOCITY;
    }

    public static boolean isProxy() {
        return platform().isProxy();
    }

    public static boolean isSpigot() {
        return platform() == Platform.SPIGOT;
    }

    public static Triton<?, ?> get() {
        return instance;
    }

    protected void onEnable() {
        instance = this;
        TritonAPIUtils.register(instance);

        translationsFolder = new File(getDataFolder(), "translations");

        logger = loader.getTritonLogger();

        config = new MainConfig(this);
        languageManager = new LanguageManager(this);
        messagesConfig = new MessagesConfig();
        translationManager = new TranslationManager(this);

        LanguageMigration.migrate();

        reload();

        twinManager = new TwinManager(this);

        if (config.isUsePacketEvents()) {
            val dependencyManager = Triton.get().getLoader().getDependencyManager();
            val isPacketEventsVendored = dependencyManager.hasLoaderFlag(LoaderFlag.VENDOR_PACKET_EVENTS);
            if (isPacketEventsVendored) {
                // load packet events dependency (netty and platform related modules are loaded later)
                dependencyManager.loadDependency(Dependency.PACKET_EVENTS_API);
            }
            this.initPacketEventsManager();
        }

        if (this.packetEventsManager != null) {
            // TODO: when spigot is supported, onLoad needs to be called earlier
            this.packetEventsManager.onLoad();
            this.packetEventsManager.onEnable();
        }
    }

    protected void onDisable() {
        // TODO: this should be called at some point
        if (this.packetEventsManager != null) {
            this.packetEventsManager.onDisable();
        }
    }

    public void reload() {
        configYAML = loadYAML("config", getConfigFileName());
        config.setup();
        dumpManager = new DumpManager();
        logger.setLogLevel(config.getLogLevel());
        if (config.getParser().equalsIgnoreCase("legacy")) {
            messageParser = new LegacyParser();
        } else {
            messageParser = new AdventureParser();
        }
        messagesConfig.setup();
        setupStorage();
        languageManager.setup();
        translationManager.setup();
        if (this.packetEventsManager != null) {
            this.packetEventsManager.onReload();
        }
        startConfigRefreshTask();
    }

    /**
     * Initialize the platform's {@link PacketEventsManager} by setting the
     * {@link Triton#packetEventsManager} variable.
     * A platform is allowed to not do anything in case PacketEvents is not supported (yet).
     *
     * @since 4.0.0
     */
    protected abstract void initPacketEventsManager();

    public void refreshPlayers() {
        playerManager.getAll().stream()
                .filter(Objects::nonNull)
                .forEach(TritonLanguagePlayer::refreshAll);
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

    /**
     * @return config filename inside the JAR without extension for the current platform
     * @since 4.0.0
     */
    protected abstract String getConfigFileName();

    private void setupStorage() {
        if (this.storage != null) {
            this.storage.unload();
        }
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
                if (this.storage != null) {
                    this.storage.unload();
                }
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

}
