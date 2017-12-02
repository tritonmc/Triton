package com.rexcantor64.multilanguageplugin.config;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.storage.SQLManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainConfig {

    private SQLManager sql = null;

    private ConfigurationSection languages;
    private String mainLanguage;
    private boolean forceLocale;
    private boolean debug;

    private String syntax;
    private String syntaxArgs;
    private String syntaxArg;

    private boolean chat;
    private boolean actionbars;
    private boolean titles;
    private boolean guis;
    private boolean scoreboards;
    private boolean scoreboardsAdvanced;
    private List<EntityType> holograms = new ArrayList<>();
    private boolean hologramsAll;
    private boolean kick;
    private boolean tab;
    private boolean items;
    private boolean inventoryItems;
    private boolean signs;

    public boolean useSql() {
        return sql != null;
    }

    public SQLManager getSql() {
        return sql;
    }

    public ConfigurationSection getLanguages() {
        return languages;
    }

    public String getMainLanguage() {
        return mainLanguage;
    }

    public boolean isForceLocale() {
        return forceLocale;
    }

    public String getSyntax() {
        return syntax;
    }

    public String getSyntaxArgs() {
        return syntaxArgs;
    }

    public String getSyntaxArg() {
        return syntaxArg;
    }

    public boolean isChat() {
        return chat;
    }

    public boolean isActionbars() {
        return actionbars;
    }

    public boolean isTitles() {
        return titles;
    }

    public boolean isGuis() {
        return guis;
    }

    public boolean isScoreboards() {
        return scoreboards;
    }

    public boolean isScoreboardsAdvanced() {
        return scoreboardsAdvanced;
    }

    public List<EntityType> getHolograms() {
        return holograms;
    }

    public boolean isHologramsAll() {
        return hologramsAll;
    }

    public boolean isKick() {
        return kick;
    }

    public boolean isTab() {
        return tab;
    }

    public boolean isItems() {
        return items;
    }

    public boolean isInventoryItems() {
        return inventoryItems;
    }

    public boolean isSigns() {
        return signs;
    }

    public boolean isDebug() {
        return debug;
    }

    private void setup(ConfigurationSection section) {
        ConfigurationSection sql = section.getConfigurationSection("storage");
        if (sql == null) sql = section.createSection("storage");
        if (sql.getBoolean("enabled", false))
            this.sql = new SQLManager(sql.getString("host", "localhost"), sql.getInt("port", 3306), sql.getString("database", "minecraft"), sql.getString("username", "root"), sql.getString("password", ""));
        this.languages = section.getConfigurationSection("languages");
        if (this.languages == null) this.languages = section.createSection("languages");
        this.mainLanguage = section.getString("main-language", "en_GB");
        this.forceLocale = section.getBoolean("force-minecraft-locale", false);
        this.debug = section.getBoolean("debug", false);
        ConfigurationSection languageCreation = section.getConfigurationSection("language-creation");
        if (languageCreation == null) languageCreation = section.createSection("language-creation");
        setupLanguageCreation(languageCreation);
        testSQLConnection();
    }

    public void setup() {
        File f = new File(SpigotMLP.get().getDataFolder(), "config.yml");
        if (!f.exists())
            SpigotMLP.get().saveResource("config.yml", false);
        setup(SpigotMLP.get().getConfig());
    }

    private void testSQLConnection() {
        //TODO test SQL connection
        //if failed, switch to YAML
    }

    private void setupLanguageCreation(ConfigurationSection section) {
        syntax = section.getString("syntax", "lang");
        syntaxArgs = section.getString("syntax-args", "args");
        syntaxArg = section.getString("syntax-arg", "arg");

        ConfigurationSection enabled = section.getConfigurationSection("enabled");
        if (enabled == null) enabled = section.createSection("enabled");
        chat = enabled.getBoolean("chat-messages", true);
        actionbars = enabled.getBoolean("action-bars", true);
        titles = enabled.getBoolean("titles", true);
        guis = enabled.getBoolean("guis", true);
        scoreboards = enabled.getBoolean("scoreboards", true);
        scoreboardsAdvanced = enabled.getBoolean("scoreboards-advanced", false);
        hologramsAll = enabled.getBoolean("holograms-allow-all", false);
        kick = enabled.getBoolean("kick", true);
        tab = enabled.getBoolean("tab", true);
        items = enabled.getBoolean("items", true);
        inventoryItems = enabled.getBoolean("inventory-items", false);
        signs = enabled.getBoolean("signs", true);

        List<String> holograms = enabled.getStringList("holograms");
        for (String hologram : holograms)
            try {
                this.holograms.add(EntityType.valueOf(hologram));
            } catch (IllegalArgumentException e) {
                SpigotMLP.get().logDebugWarning("Failed to register hologram type %1 because it's not a valid entity type! Please check your spelling and if you can't fix it, please contact the developer!", hologram);
            }
    }

}
