package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.TritonConfig;
import com.rexcantor64.triton.api.wrappers.EntityType;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class MainConfig implements TritonConfig {

    private final Triton main;
    private Configuration languages;
    private String mainLanguage;
    private boolean runLanguageCommandsOnLogin;
    private boolean alwaysCheckClientLocale;
    private boolean debug;
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

    public MainConfig(Triton main) {
        this.main = main;
    }

    public Configuration getLanguages() {
        return languages;
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

    public String getMainLanguage() {
        return mainLanguage;
    }

    public void setMainLanguage(String mainLanguage) {
        this.mainLanguage = mainLanguage;
    }

    public boolean isRunLanguageCommandsOnLogin() {
        return runLanguageCommandsOnLogin;
    }

    public boolean isAlwaysCheckClientLocale() {
        return alwaysCheckClientLocale;
    }

    public boolean isBungeecord() {
        return bungeecord;
    }

    public int getConfigAutoRefresh() {
        return configAutoRefresh;
    }

    public String getTwinToken() {
        return twinToken;
    }

    public String getDisabledLine() {
        return disabledLine;
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

    public boolean isBooks() {
        return books;
    }

    public boolean isSigns() {
        return signs;
    }

    public boolean isBossbars() {
        return bossbars;
    }

    @Override
    public boolean isMotd() {
        return motd;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public boolean isDebug() {
        return debug;
    }

    public FeatureSyntax getChatSyntax() {
        return chatSyntax;
    }

    public FeatureSyntax getActionbarSyntax() {
        return actionbarSyntax;
    }

    public FeatureSyntax getTitleSyntax() {
        return titleSyntax;
    }

    public FeatureSyntax getGuiSyntax() {
        return guiSyntax;
    }

    public FeatureSyntax getScoreboardSyntax() {
        return scoreboardSyntax;
    }

    public FeatureSyntax getHologramSyntax() {
        return hologramSyntax;
    }

    public FeatureSyntax getKickSyntax() {
        return kickSyntax;
    }

    public FeatureSyntax getTabSyntax() {
        return tabSyntax;
    }

    public FeatureSyntax getItemsSyntax() {
        return itemsSyntax;
    }

    public FeatureSyntax getSignsSyntax() {
        return signsSyntax;
    }

    public FeatureSyntax getBossbarSyntax() {
        return bossbarSyntax;
    }

    @Override
    public FeatureSyntax getMotdSyntax() {
        return motdSyntax;
    }

    private void setup(Configuration section) {
        this.bungeecord = section.getBoolean("bungeecord", false);
        if (!this.bungeecord) {
            this.languages = section.getSection("languages");
            this.mainLanguage = section.getString("main-language", "en_GB");
        }
        this.twinToken = section.getString("twin-token", "");
        this.runLanguageCommandsOnLogin = section.getBoolean("run-language-commands-on-join", false);
        this.alwaysCheckClientLocale = section.getBoolean("force-client-locale-on-join", false);
        this.debug = section.getBoolean("debug", false);
        this.configAutoRefresh = section.getInt("config-auto-refresh-interval", -1);
        Configuration languageCreation = section.getSection("language-creation");
        setupLanguageCreation(languageCreation);
    }

    public void setup() {
        setup(main.getConfig());
        if (this.bungeecord)
            setupFromCache();
    }

    private void setupFromCache() {
        File file = new File(Triton.get().getDataFolder(), "cache.json");
        if (!file.exists()) {
            try {
                Files.write(file.toPath(), "{}".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (Exception e) {
                Triton.get().logDebugWarning("Failed to create %1! Error: %2", file.getAbsolutePath(), e.getMessage());
            }
            return;
        }
        try {
            JSONObject obj = new JSONObject(FileUtils.contentsToString(file));
            setLanguages(obj);
        } catch (JSONException e) {
            Triton.get().logWarning("Failed to load languages from cache.json! Invalid JSON format: %1",
                    e.getMessage());
        }
    }

    private void setupLanguageCreation(Configuration section) {
        disabledLine = section.getString("disabled-line", "");
        terminal = section.getBoolean("terminal", false);

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
                main.logDebugWarning("Failed to register hologram type %1 because it's not a valid entity type! " +
                        "Please check your spelling and if you can't fix it, please contact the developer!", hologram);
            }
    }

    public static class FeatureSyntax implements com.rexcantor64.triton.api.config.FeatureSyntax {
        private String lang;
        private String args;
        private String arg;
        private boolean interactive = false;

        private FeatureSyntax(String lang, String args, String arg) {
            this.lang = lang;
            this.args = args;
            this.arg = arg;
        }

        private static FeatureSyntax fromSection(Configuration section) {
            return new FeatureSyntax(section.getString("syntax-lang", "lang"), section.getString("syntax-args", "args"
            ), section.getString("syntax-arg", "arg"));
        }

        public String getLang() {
            return lang;
        }

        public String getArgs() {
            return args;
        }

        public String getArg() {
            return arg;
        }

        public int getPatternSize() {
            return lang.length() + 2;
        }

        public int getPatternArgSize() {
            return arg.length() + 2;
        }

        public boolean isInteractive() {
            return interactive;
        }
    }

}
