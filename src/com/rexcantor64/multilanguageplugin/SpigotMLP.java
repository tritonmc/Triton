package com.rexcantor64.multilanguageplugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.rexcantor64.multilanguageplugin.commands.MainCMD;
import com.rexcantor64.multilanguageplugin.config.LanguageConfig;
import com.rexcantor64.multilanguageplugin.config.MainConfig;
import com.rexcantor64.multilanguageplugin.guiapi.GuiManager;
import com.rexcantor64.multilanguageplugin.language.LanguageManager;
import com.rexcantor64.multilanguageplugin.language.LanguageParser;
import com.rexcantor64.multilanguageplugin.migration.LanguageMigration;
import com.rexcantor64.multilanguageplugin.packetinterceptor.ProtocolLibListener;
import com.rexcantor64.multilanguageplugin.player.PlayerManager;
import com.rexcantor64.multilanguageplugin.web.GistUploader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class SpigotMLP extends JavaPlugin {

    // Create the instance
    private static SpigotMLP instance;

    // File-related variables
    private File languageFolder;

    // Configs
    private MainConfig config;
    private LanguageConfig languageConfig;
    private YamlConfiguration messagesConfig;

    // Managers
    private LanguageManager languageManager;
    private LanguageParser languageParser;
    private PlayerManager playerManager;
    private GuiManager guiManager;
    private GistUploader gistUploader;

    @Override
    public void onEnable() {
        instance = this;
        languageFolder = new File(getDataFolder(), "languages");
        // Setup config.yml
        (config = new MainConfig(this)).setup();
        // Setup messages.yml
        File f = new File(getDataFolder(), "messages.yml");
        if (!f.exists())
            saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(f);
        // Start migration. Remove on v1.1.0.
        LanguageMigration.migrate();
        // Setup more classes
        (languageConfig = new LanguageConfig()).setup();
        (languageManager = new LanguageManager()).setup();
        playerManager = new PlayerManager();
        languageParser = new LanguageParser();
        guiManager = new GuiManager();
        gistUploader = new GistUploader(this);
        // Setup commands
        getCommand("multilanguageplugin").setExecutor(new MainCMD());
        // Setup listeners
        Bukkit.getPluginManager().registerEvents(guiManager, this);
        // Use ProtocolLib if available
        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibListener(this));
    }

    @Override
    public void onDisable() {

    }

    public void reload() {
        reloadConfig();
        config.setup();
        File f = new File(getDataFolder(), "messages.yml");
        if (!f.exists())
            saveResource("messages.yml", false);
        messagesConfig = YamlConfiguration.loadConfiguration(f);
        languageConfig.setup();
        languageManager.setup();
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

    public GistUploader getGistUploader() {
        return gistUploader;
    }

    public String getMessage(String code, Object... args) {
        String s = ChatColor.translateAlternateColorCodes('&',
                messagesConfig.getString(code, "Error while getting a message from the messages file!"));
        for (int i = 0; i < args.length; i++)
            if (args[i] != null)
                s = s.replace("%" + (i + 1), args[i].toString());
        return s;
    }

    public List<String> getMessageList(String code) {
        return messagesConfig.getStringList(code);
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
        getLogger().log(Level.INFO, info);
    }

    public void logWarning(String warning, Object... arguments) {
        if (warning == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                warning = warning.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        getLogger().log(Level.WARNING, warning);
    }

    public void logError(String error, Object... arguments) {
        if (error == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                error = error.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        getLogger().log(Level.SEVERE, error);
    }

    public void logDebug(String info, Object... arguments) {
        if (info == null) return;
        if (!config.isDebug()) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                info = info.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        getLogger().log(Level.INFO, "[DEBUG] " + info);
    }

    public void logDebugWarning(String warning, Object... arguments) {
        if (!config.isDebug()) return;
        if (warning == null) return;
        for (int i = 0; i < arguments.length; i++)
            if (arguments[i] != null)
                warning = warning.replace("%" + Integer.toString(i + 1), arguments[i].toString());
        getLogger().log(Level.WARNING, "[DEBUG] " + warning);
    }

    public static SpigotMLP get() {
        return instance;
    }

}
