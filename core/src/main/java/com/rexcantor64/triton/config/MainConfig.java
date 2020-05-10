package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.TritonConfig;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.utils.FileUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MainConfig implements TritonConfig {

    private final Triton main;
    private Configuration languages;
    private String mainLanguage;
    private boolean runLanguageCommandsOnLogin;
    private boolean alwaysCheckClientLocale;
    private int logLevel;
    private boolean bungeecord;
    private int configAutoRefresh;
    private String twinToken;
    private String disabledLine;
    private boolean chat;
    private FeatureSyntax chatSyntax;
    private boolean actionbars;
    private FeatureSyntax actionbarSyntax;
    private boolean titles;
    private FeatureSyntax titleSyntax;
    private boolean guis;
    private FeatureSyntax guiSyntax;
    private boolean scoreboards;
    private boolean scoreboardsAdvanced;
    private FeatureSyntax scoreboardSyntax;
    private List<EntityType> holograms = new ArrayList<>();
    private boolean hologramsAll;
    private FeatureSyntax hologramSyntax;
    private boolean kick;
    private FeatureSyntax kickSyntax;
    private boolean tab;
    private FeatureSyntax tabSyntax;
    private boolean items;
    private boolean inventoryItems;
    private boolean books;
    private FeatureSyntax itemsSyntax;
    private boolean signs;
    private FeatureSyntax signsSyntax;
    private boolean bossbars;
    private FeatureSyntax bossbarSyntax;
    private boolean motd;
    private FeatureSyntax motdSyntax;
    private boolean terminal;
    private boolean terminalAnsi;

    private boolean mysql = false;
    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUser;
    private String mysqlPassword;
    private String mysqlTablePrefix;

    public MainConfig(Triton main) {
        this.main = main;
    }

    public void setLanguages(Configuration languages) {
        this.languages = languages;
    }

    public void setLanguages(JSONObject languages) {
        Configuration configuration = new Configuration();
        for (String lang : languages.keySet()) {
            JSONObject json = languages.optJSONObject(lang);
            if (json == null) continue;
            Configuration section = configuration.getSection(lang);
            section.set("flag", json.optString("flag"));
            List<String> minecraftCodes = new ArrayList<>();
            JSONArray minecraftCodesJSON = json.optJSONArray("minecraft-code");
            if (minecraftCodesJSON != null)
                for (int i = 0; i < minecraftCodesJSON.length(); i++)
                    minecraftCodes.add(minecraftCodesJSON.optString(i));
            section.set("minecraft-code", minecraftCodes);
            section.set("display-name", json.optString("display-name"));
            if (json.optBoolean("main"))
                setMainLanguage(lang);
        }
        this.languages = configuration;
    }

    public void setMainLanguage(String mainLanguage) {
        this.mainLanguage = mainLanguage;
    }

    private void setup(Configuration section) {
        this.bungeecord = section.getBoolean("bungeecord", false);
        if (!this.bungeecord) {
            this.languages = section.getSection("languages");
            this.mainLanguage = section.getString("main-language", "en_GB");

            Configuration database = section.getSection("database");
            mysql = database.getBoolean("enabled", false);
            mysqlHost = database.getString("host", "localhost");
            mysqlPort = database.getInt("port", 3306);
            mysqlDatabase = database.getString("database", "triton");
            mysqlUser = database.getString("username", "root");
            mysqlPassword = database.getString("password", "");
            mysqlTablePrefix = database.getString("table-prefix", "triton_");
        }
        this.twinToken = section.getString("twin-token", "");
        this.runLanguageCommandsOnLogin = section.getBoolean("run-language-commands-on-join", false);
        this.alwaysCheckClientLocale = section.getBoolean("force-client-locale-on-join", false);
        this.logLevel = section.getInt("log-level", 0);
        this.configAutoRefresh = section.getInt("config-auto-refresh-interval", -1);
        Configuration languageCreation = section.getSection("language-creation");
        setupLanguageCreation(languageCreation);
    }

    public void setup() {
        setup(main.getConfigYAML());
        if (this.bungeecord)
            setupFromCache();
    }

    private void setupFromCache() {
        File file = new File(Triton.get().getDataFolder(), "cache.json");
        if (!file.exists()) {
            try {
                Files.write(file.toPath(), "{}".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (Exception e) {
                Triton.get().getLogger()
                        .logDebugWarning("Failed to create %1! Error: %2", file.getAbsolutePath(), e.getMessage());
            }
            return;
        }
        try {
            JSONObject obj = new JSONObject(FileUtils.contentsToString(file));
            setLanguages(obj);
        } catch (JSONException e) {
            Triton.get().getLogger().logWarning("Failed to load languages from cache.json! Invalid JSON format: %1",
                    e.getMessage());
        }
    }

    private void setupLanguageCreation(Configuration section) {
        disabledLine = section.getString("disabled-line", "");
        terminal = section.getBoolean("terminal", false);
        terminalAnsi = section.getBoolean("terminalAnsi", true);

        Configuration chat = section.getSection("chat");
        this.chat = chat.getBoolean("enabled", true);
        this.chatSyntax = FeatureSyntax.fromSection(chat);
        this.chatSyntax.interactive = true;

        Configuration actionbars = section.getSection("actionbars");
        this.actionbars = actionbars.getBoolean("enabled", true);
        this.actionbarSyntax = FeatureSyntax.fromSection(actionbars);

        Configuration titles = section.getSection("titles");
        this.titles = titles.getBoolean("enabled", true);
        this.titleSyntax = FeatureSyntax.fromSection(titles);

        Configuration guis = section.getSection("guis");
        this.guis = guis.getBoolean("enabled", true);
        this.guiSyntax = FeatureSyntax.fromSection(guis);

        Configuration scoreboards = section.getSection("scoreboards");
        this.scoreboards = scoreboards.getBoolean("enabled", false);
        this.scoreboardsAdvanced = scoreboards.getBoolean("advanced", false);
        this.scoreboardSyntax = FeatureSyntax.fromSection(scoreboards);

        Configuration holograms = section.getSection("holograms");
        this.hologramsAll = holograms.getBoolean("allow-all", false);
        this.hologramSyntax = FeatureSyntax.fromSection(holograms);

        Configuration kick = section.getSection("kick");
        this.kick = kick.getBoolean("enabled", true);
        this.kickSyntax = FeatureSyntax.fromSection(kick);

        Configuration tab = section.getSection("tab");
        this.tab = tab.getBoolean("enabled", true);
        this.tabSyntax = FeatureSyntax.fromSection(tab);

        Configuration items = section.getSection("items");
        this.items = items.getBoolean("enabled", true);
        this.inventoryItems = items.getBoolean("allow-in-inventory", false);
        this.books = items.getBoolean("books", false);
        this.itemsSyntax = FeatureSyntax.fromSection(items);

        Configuration signs = section.getSection("signs");
        this.signs = signs.getBoolean("enabled", true);
        this.signsSyntax = FeatureSyntax.fromSection(signs);

        Configuration bossbars = section.getSection("bossbars");
        this.bossbars = bossbars.getBoolean("enabled", true);
        this.bossbarSyntax = FeatureSyntax.fromSection(bossbars);

        Configuration motd = section.getSection("motd");
        this.motd = motd.getBoolean("enabled", true);
        this.motdSyntax = FeatureSyntax.fromSection(motd);

        List<String> hologramList = holograms.getStringList("types");
        for (String hologram : hologramList)
            try {
                this.holograms.add(EntityType.valueOf(hologram.toUpperCase()));
            } catch (IllegalArgumentException e) {
                main.getLogger()
                        .logWarning("Failed to register hologram type %1 because it's not a valid entity type! " +
                                        "Please check your spelling and if you can't fix it, please contact the " +
                                        "developer!",
                                hologram);
            }
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FeatureSyntax implements com.rexcantor64.triton.api.config.FeatureSyntax {
        private final String lang;
        private final String args;
        private final String arg;
        private boolean interactive = false;

        private static FeatureSyntax fromSection(Configuration section) {
            return new FeatureSyntax(
                    section.getString("syntax-lang", "lang"),
                    section.getString("syntax-args", "args"),
                    section.getString("syntax-arg", "arg")
            );
        }

    }

}
