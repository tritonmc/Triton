package com.rexcantor64.triton.config;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.TritonConfig;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.utils.YAMLUtils;
import lombok.Cleanup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@ToString
public class MainConfig implements TritonConfig {

    private final static JsonParser JSON_PARSER = new JsonParser();
    private final static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LANGUAGES_TYPE = new TypeToken<List<Language>>() {
    }.getType();

    @ToString.Exclude
    private transient final Triton<?, ?> main;
    @Setter
    private List<Language> languages;
    @Setter
    private String mainLanguage;
    private String openSelectorCommandOverride;
    private boolean runLanguageCommandsOnLogin;
    private boolean alwaysCheckClientLocale;
    private int logLevel;
    private boolean bungeecord;
    private int configAutoRefresh;
    private String twinToken;
    private List<String> commandAliases;
    private String disabledLine;
    private boolean chat;
    private boolean signedChat;
    private FeatureSyntax chatSyntax;
    private boolean actionbars;
    private FeatureSyntax actionbarSyntax;
    private boolean titles;
    private FeatureSyntax titleSyntax;
    private boolean guis;
    private FeatureSyntax guiSyntax;
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
    private boolean scoreboards;
    private FeatureSyntax scoreboardSyntax;
    private boolean advancements;
    private FeatureSyntax advancementsSyntax;
    private boolean advancementsRefresh;
    private boolean resourcePackPrompt;
    private FeatureSyntax resourcePackPromptSyntax;
    private boolean deathScreen;
    private FeatureSyntax deathScreenSyntax;
    private boolean terminal;
    private boolean terminalAnsi;
    private boolean preventPlaceholdersInChat;
    private int maxPlaceholdersInMessage;
    private boolean asyncProtocolLib;

    private String storageType = "local";
    private String serverName;
    @ToString.Exclude
    private String databaseHost;
    @ToString.Exclude
    private int databasePort;
    @ToString.Exclude
    private String databaseName;
    @ToString.Exclude
    private String databaseUser;
    @ToString.Exclude
    private String databasePassword;
    @ToString.Exclude
    private String databaseTablePrefix;
    private int databaseMysqlPoolMaxSize;
    private int databaseMysqlPoolMinIdle;
    private long databaseMysqlPoolMaxLifetime;
    private long databaseMysqlPoolConnTimeout;
    private Map<String, String> databaseMysqlPoolProperties;
    private boolean iKnowWhatIAmDoing;
    private String twinInstance;

    public MainConfig(Triton<?, ?> main) {
        this.main = main;
    }

    private void setup(Configuration section) {
        this.bungeecord = section.getBoolean("bungeecord", false);
        if (Triton.isProxy() || !this.bungeecord) {
            this.twinToken = section.getString("twin-token", "");
        }

        val languagesSection = section.getSection("languages");
        val languages = new ArrayList<Language>();
        if (languagesSection != null) {
            for (String lang : languagesSection.getKeys())
                languages.add(new Language(
                        lang,
                        languagesSection.getString(lang + ".flag", "pa"),
                        YAMLUtils.getStringOrStringList(languagesSection, lang + ".minecraft-code"),
                        languagesSection.getString(lang + ".display-name", "&4Unknown"),
                        languagesSection.getStringList(lang + ".fallback-languages"),
                        languagesSection.getStringList(lang + ".commands")));
        } else {
            languages.add(new Language("temp", "pabk", new ArrayList<>(), "Error", null, null));
        }
        this.languages = languages;

        this.mainLanguage = section.getString("main-language", "en_GB");
        this.openSelectorCommandOverride = section.getString("open-selector-command-override", null);
        Configuration database = section.getSection("storage");
        storageType = database.getString("type", "local");
        serverName = database.getString("server-name", "lobby");
        databaseHost = database.getString("host", "localhost");
        databasePort = database.getInt("port", 3306);
        databaseName = database.getString("database", "triton");
        databaseUser = database.getString("username", "root");
        databasePassword = database.getString("password", "");
        databaseTablePrefix = database.getString("table-prefix", "triton_");

        val databaseMysqlPool = database.getSection("mysql-pool-advanced");
        databaseMysqlPoolMaxSize = databaseMysqlPool.getInt("maximum-pool-size", 10);
        databaseMysqlPoolMinIdle = databaseMysqlPool.getInt("minimum-idle", 10);
        databaseMysqlPoolMaxLifetime = databaseMysqlPool.getLong("maximum-lifetime", 1800000);
        databaseMysqlPoolConnTimeout = databaseMysqlPool.getLong("connection-timeout", 5000);
        val properties = databaseMysqlPool.getSection("properties");
        databaseMysqlPoolProperties = new HashMap<>();
        for (val key : properties.getKeys())
            databaseMysqlPoolProperties.put(key, Objects.toString(properties.get(key), ""));
        databaseMysqlPoolProperties.putIfAbsent("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
        databaseMysqlPoolProperties.putIfAbsent("cachePrepStmts", "true");
        databaseMysqlPoolProperties.putIfAbsent("prepStmtCacheSize", "250");
        databaseMysqlPoolProperties.putIfAbsent("prepStmtCacheSqlLimit", "2048");

        if (section.contains("command-aliases"))
            commandAliases = section.getStringList("command-aliases");
        else
            commandAliases = Lists.newArrayList("lang", "language");

        this.runLanguageCommandsOnLogin = section.getBoolean("run-language-commands-on-join", false);
        this.alwaysCheckClientLocale = section.getBoolean("force-client-locale-on-join", false);
        this.logLevel = section.getInt("log-level", 0);
        this.configAutoRefresh = section.getInt("config-auto-refresh-interval", -1);
        this.asyncProtocolLib = section.getBoolean("experimental-async-protocol-lib", false);
        Configuration languageCreation = section.getSection("language-creation");
        setupLanguageCreation(languageCreation);

        this.iKnowWhatIAmDoing = section.getBoolean("i-know-what-i-am-doing", false);
        this.twinInstance = section.getString("twin-instance", "https://twin.rexcantor64.com");
    }

    public void setup() {
        setup(main.getConfigYAML());
        if (Triton.isSpigot() && this.bungeecord && storageType.equalsIgnoreCase("local"))
            setupFromCache();
    }

    private void setupFromCache() {
        File file = new File(Triton.get().getDataFolder(), "cache.json");
        if (!file.exists()) {
            try {
                Files.write(file.toPath(), "{}".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (Exception e) {
                Triton.get().getLogger()
                        .logError(e, "Failed to create %1!", file.getAbsolutePath());
            }
            return;
        }
        try {
            @Cleanup val reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            val json = JSON_PARSER.parse(reader);
            if (!json.isJsonObject()) {
                Triton.get().getLogger().logWarning("Could not load languages from cache. JSON isn't a JSON object.");
                return;
            }
            val mainLang = json.getAsJsonObject().getAsJsonPrimitive("mainLanguage");
            if (mainLang == null || mainLang.getAsString() == null) {
                Triton.get().getLogger()
                        .logWarning("Could not load languages from cache. `mainLanguage` is not a string");
                return;
            }
            this.mainLanguage = mainLang.getAsString();
            setLanguages(gson.fromJson(json.getAsJsonObject().getAsJsonArray("languages"), LANGUAGES_TYPE));
            this.languages.forEach(Language::computeProperties);
        } catch (Exception e) {
            Triton.get().getLogger().logWarning("Failed to load languages from cache.json! Invalid JSON format: %1",
                    e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupLanguageCreation(Configuration section) {
        disabledLine = section.getString("disabled-line", "");
        terminal = section.getBoolean("terminal", false);
        terminalAnsi = section.getBoolean("terminalAnsi", true);
        preventPlaceholdersInChat = section.getBoolean("prevent-placeholders-in-chat", true);
        maxPlaceholdersInMessage = section.getInt("max-placeholders-in-message", 10);

        Configuration chat = section.getSection("chat");
        this.chat = chat.getBoolean("enabled", true);
        this.signedChat = chat.getBoolean("signed-enabled", true);
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

        Configuration scoreboards = section.getSection("scoreboards");
        this.scoreboards = scoreboards.getBoolean("enabled", true);
        this.scoreboardSyntax = FeatureSyntax.fromSection(scoreboards);

        Configuration advancements = section.getSection("advancements");
        this.advancements = advancements.getBoolean("enabled", false);
        this.advancementsSyntax = FeatureSyntax.fromSection(advancements);
        this.advancementsRefresh = advancements.getBoolean("experimental-advancements-refresh", false);

        Configuration resourcePackPrompt = section.getSection("resource-pack-prompt");
        this.resourcePackPrompt = resourcePackPrompt.getBoolean("enabled", true);
        this.resourcePackPromptSyntax = FeatureSyntax.fromSection(resourcePackPrompt);

        Configuration deathScreen = section.getSection("death-screen");
        this.deathScreen = deathScreen.getBoolean("enabled", true);
        this.deathScreenSyntax = FeatureSyntax.fromSection(deathScreen);

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

    @Override
    public boolean isMysql() {
        // Handle deprecated API
        return storageType.equalsIgnoreCase("mysql");
    }

    @Override
    public boolean isScoreboardsAdvanced() {
        // Handle deprecated API
        return false;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
        main.getLogger().setLogLevel(logLevel);
    }

    @Getter
    @RequiredArgsConstructor
    @ToString
    @VisibleForTesting
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
