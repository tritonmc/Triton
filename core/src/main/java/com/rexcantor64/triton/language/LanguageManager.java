package com.rexcantor64.triton.language;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.SignLocation;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import lombok.var;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageManager implements com.rexcantor64.triton.api.language.LanguageManager {

    private List<Language> languages = new ArrayList<>();
    private Language mainLanguage;
    private HashMap<String, HashMap<String, String>> textItems = new HashMap<>();
    private HashMap<String, HashMap<SignLocation, String[]>> signItems = new HashMap<>();
    @Getter
    private List<String> signKeys = new ArrayList<>();
    private Map<Pattern, LanguageText> matches = new HashMap<>();
    @Getter
    private int itemCount = 0;

    public String matchPattern(String input, LanguagePlayer p) {
        return matchPattern(input, p.getLang().getName());
    }

    public String matchPattern(String input, String language) {
        for (Map.Entry<Pattern, LanguageText> entry : matches.entrySet()) {
            String replacement = entry.getValue().getMessageRegex(language);
            if (replacement == null) replacement = entry.getValue().getMessageRegex(mainLanguage.getName());
            if (replacement == null) continue;
            Matcher matcher = entry.getKey().matcher(input);
            input = matcher.replaceAll(ChatColor.translateAlternateColorCodes('&', replacement));
        }
        return input;
    }

    public String getText(@NonNull LanguagePlayer p, String code, Object... args) {
        return getText(p.getLang().getName(), code, args);
    }

    public String getText(@NonNull String language, @NonNull String code, @NonNull Object... args) {
        val langItems = this.textItems.get(language);
        if (langItems == null) return getTextFromMain(code, args);

        val msg = langItems.get(code);
        if (msg == null) return getTextFromMain(code, args);

        return formatMessage(msg, args);
    }

    public String getTextFromMain(@NonNull String code, @NonNull Object... args) {
        val langItems = this.textItems.get(this.getMainLanguage().getName());
        if (langItems == null) return Triton.get().getMessagesConfig().getMessage("error.message-not-found", code);

        val msg = langItems.get(code);
        if (msg == null) return Triton.get().getMessagesConfig().getMessage("error.message-not-found", code);

        return formatMessage(msg, args);
    }

    private String formatMessage(@NonNull String msg, @NonNull Object... args) {
        for (int i = 0; i < args.length; ++i)
            msg = msg.replace("%" + (i + 1), String.valueOf(args[i]));
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String[] getSign(LanguagePlayer player, SignLocation location) {
        return getSign(player, location, () -> new String[4]);
    }

    public String[] getSign(@NonNull LanguagePlayer player, SignLocation location, String[] defaultLines) {
        return getSign(player.getLang().getName(), location, () -> defaultLines);
    }

    public String[] getSign(@NonNull LanguagePlayer player, SignLocation location, Supplier<String[]> defaultLines) {
        return getSign(player.getLang().getName(), location, defaultLines);
    }

    public String[] getSign(@NonNull String language, @NonNull SignLocation location,
                            @NonNull Supplier<String[]> defaultLines) {
        val langItems = this.signItems.get(language);
        if (langItems == null) return getSignFromMain(location, defaultLines);

        var lines = langItems.get(location);
        if (lines == null) return getSignFromMain(location, defaultLines);

        return formatLines(language, lines, defaultLines);
    }

    private String[] getSignFromMain(@NonNull SignLocation location, @NonNull Supplier<String[]> defaultLines) {
        val langItems = this.signItems.get(this.mainLanguage.getName());
        if (langItems == null) return null;

        var lines = langItems.get(location);
        if (lines == null) return null;

        return formatLines(this.mainLanguage.getName(), lines, defaultLines);
    }

    public String[] formatLines(@NonNull String language, @NonNull String[] lines,
                                @NonNull Supplier<String[]> defaultLinesSupplier) {
        val result = new String[4];
        String[] defaultLines = null;

        for (int i = 0; i < 4; ++i) {
            if (lines.length - 1 < i) {
                result[i] = "";
                continue;
            }
            result[i] = lines[i];

            if (result[i].equals("%use_line_default%")) {
                if (defaultLines == null) {
                    defaultLines = defaultLinesSupplier.get();
                }
                result[i] = Triton.get().getLanguageParser()
                        .replaceLanguages(
                                matchPattern(defaultLines.length > i && defaultLines[i] != null ? defaultLines[i] :
                                        "", language),
                                language,
                                Triton.get().getConf().getSignsSyntax()
                        );
                while (result[i].startsWith(ChatColor.RESET.toString()))
                    result[i] = result[i].substring(2);
            }

            result[i] = ChatColor.translateAlternateColorCodes('&', result[i]);
        }

        return result;
    }

    public Language getLanguageByName(String name, boolean fallback) {
        if (name != null)
            for (Language lang : languages)
                if (lang.getName().equals(name))
                    return lang;
        if (fallback) return mainLanguage;
        return null;
    }

    public Language getLanguageByLocale(String name, boolean fallback) {
        if (name != null)
            for (Language lang : languages)
                for (String s : lang.getMinecraftCodes())
                    if (s.equalsIgnoreCase(name))
                        return lang;
        if (fallback) return mainLanguage;
        return null;
    }

    public List<com.rexcantor64.triton.api.language.Language> getAllLanguages() {
        return new ArrayList<>(languages);
    }

    public Language getMainLanguage() {
        return mainLanguage;
    }

    public void setup() {
        Triton.get().getLogger().logInfo(1, "Setting up language manager...");

        val languages = Triton.get().getConf().getLanguages();
        val mainLang = Triton.get().getConf().getMainLanguage();

        for (val lang : languages)
            if (lang.getName().equals(mainLang))
                this.mainLanguage = lang;
        if (this.mainLanguage == null) this.mainLanguage = languages.get(0);

        this.languages = languages;

        // Map<Language Name, Map<Translation Key, Text>>
        val textItems = new HashMap<String, HashMap<String, String>>();
        // Map<Language Name, Map<Sign Location, Lines>>
        val signItems = new HashMap<String, HashMap<SignLocation, String[]>>();
        val signKeys = new ArrayList<String>();

        Map<Pattern, LanguageText> matches = new HashMap<>();

        val filterItems = Triton.get() instanceof SpigotMLP && Triton.get().getConfig().isBungeecord() && !(Triton.get()
                .getStorage() instanceof LocalStorage);
        val serverName = Triton.get().getConfig().getServerName();

        var itemCount = 0;
        for (val collection : Triton.get().getStorage().getCollections().values()) {

            for (val item : collection.getItems()) {
                if (item.getTwinData() != null && item.getTwinData().isArchived()) continue;

                if (item instanceof LanguageText) {
                    val itemText = (LanguageText) item;
                    if (filterItems && !itemText.belongsToServer(collection.getMetadata(), serverName)) continue;

                    if (itemText.getPatterns() != null) {
                        itemText.getPatterns().forEach((pattern) -> matches.put(Pattern.compile(pattern), itemText));
                        itemText.generateRegexStrings();
                    }

                    if (itemText.getLanguages() != null)
                        itemText.getLanguages().forEach((key, value) -> {
                            if (!textItems.containsKey(key)) textItems.put(key, new HashMap<>());
                            textItems.get(key).put(itemText.getKey(), value);
                        });
                }
                if (item instanceof LanguageSign) {
                    val itemSign = (LanguageSign) item;
                    signKeys.add(itemSign.getKey());
                    if (itemSign.getLines() != null && itemSign.getLocations() != null)
                        itemSign.getLines().forEach((key, value) -> {
                            if (!signItems.containsKey(key)) signItems.put(key, new HashMap<>());

                            val signLang = signItems.get(key);
                            itemSign.getLocations().stream()
                                    .filter((loc) -> !filterItems || loc.getServer() == null || serverName
                                            .equals(loc.getServer()))
                                    .forEach((loc) -> signLang.put(loc, value));
                        });
                }
                itemCount++;
            }
        }

        this.textItems = textItems;
        this.signItems = signItems;
        this.signKeys = signKeys;
        this.matches = matches;
        this.itemCount = itemCount;

        Triton.get().getLogger()
                .logInfo(1, "Successfully setup the language manager! %1 languages and %2 language items loaded!",
                        this.languages.size(), itemCount);
    }

}
