package com.rexcantor64.triton.language;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.language.SignLocation;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class TranslationManager implements com.rexcantor64.triton.api.language.TranslationManager {

    private static final String MINIMESSAGE_TYPE_TAG = "[minimsg]";
    private static final String JSON_TYPE_TAG = "[triton_json]";

    private final Triton<?, ?> triton;

    // We have to store these as Strings and only convert to Component afterwards because
    // of PlaceholderAPI: https://github.com/PlaceholderAPI/PlaceholderAPI/discussions/742
    // Map<Language Name, Map<Translation Key, Text>>
    private Map<Language, HashMap<String, String>> textItems = new HashMap<>();
    private Map<Language, HashMap<SignLocation, String[]>> signItems = new HashMap<>();
    @Getter
    private List<String> signKeys = new ArrayList<>();
    private Map<Pattern, LanguageText> matches = new HashMap<>();
    @Getter
    private int textTranslationCount = 0;
    @Getter
    private int signTranslationCount = 0;

    private Component translationNotFoundComponent = Component.empty();

    public synchronized void setup() {
        this.translationNotFoundComponent = this.triton.getMessagesConfig().getMessageComponent("error.message-not-found");

        // Map<Language, Map<Translation Key, Text>>
        val textItems = new HashMap<Language, HashMap<String, String>>();
        // Map<Language Name, Map<Sign Location, Lines>>
        val signItems = new HashMap<Language, HashMap<SignLocation, String[]>>();
        val signKeys = new ArrayList<String>();

        Map<Pattern, LanguageText> matches = new HashMap<>();

        // Only filter items if using MySQL
        val filterItems = Triton.isSpigot() && this.triton.getConfig().isBungeecord()
                && !(this.triton.getStorage() instanceof LocalStorage);
        val serverName = this.triton.getConfig().getServerName();

        int textTranslationCount = 0;
        int signTranslationCount = 0;
        for (val collection : this.triton.getStorage().getCollections().values()) {
            for (val item : collection.getItems()) {
                if (item.getTwinData() != null && item.getTwinData().isArchived()) {
                    continue;
                }

                if (item instanceof LanguageText) {
                    val itemText = (LanguageText) item;
                    if (filterItems && !itemText.belongsToServer(collection.getMetadata(), serverName)) continue;

                    if (itemText.getPatterns() != null) {
                        itemText.getPatterns().forEach((pattern) -> matches.put(Pattern.compile(pattern), itemText));
                        itemText.generateRegexStrings();
                    }

                    if (itemText.getLanguages() != null) {
                        itemText.getLanguages().forEach((key, value) -> {
                            val language = this.triton.getLanguageManager().getLanguageByName(key);
                            if (!language.isPresent()) {
                                return;
                            }

                            textItems.computeIfAbsent(language.get(), l -> new HashMap<>()).put(itemText.getKey(), value);
                        });
                    }

                    textTranslationCount += 1;
                }
                if (item instanceof LanguageSign) {
                    val itemSign = (LanguageSign) item;
                    signKeys.add(itemSign.getKey());
                    if (itemSign.getLines() != null && itemSign.getLocations() != null) {
                        itemSign.getLines().forEach((key, value) -> {
                            val language = this.triton.getLanguageManager().getLanguageByName(key);
                            if (!language.isPresent()) {
                                return;
                            }

                            val signLang = signItems.computeIfAbsent(language.get(), l -> new HashMap<>());
                            itemSign.getLocations().stream()
                                    .filter((loc) -> !filterItems || loc.getServer() == null || serverName
                                            .equals(loc.getServer()))
                                    .forEach((loc) -> signLang.put(loc, value));
                        });
                    }

                    signTranslationCount += 1;
                }
            }
        }

        this.textItems = textItems;
        this.signItems = signItems;
        this.signKeys = Collections.unmodifiableList(signKeys);
        this.matches = Collections.unmodifiableMap(matches);
        this.textTranslationCount = textTranslationCount;
        this.signTranslationCount = signTranslationCount;


        this.triton.getLogger()
                .logInfo(
                        "Successfully setup the Translation Manager! %1 text translations and %2 sign translations loaded!",
                        textTranslationCount,
                        signTranslationCount
                );
    }

    @Override
    public @NotNull Component getTextComponentOr404(@NotNull Localized locale, @NotNull String key, Component... arguments) {
        return getTextComponent(locale, key, arguments)
                .orElseGet(() -> getTranslationNotFoundComponent(key, arguments));
    }

    @Override
    public @NotNull Optional<Component> getTextComponent(@NotNull Localized locale, @NotNull String key, Component... arguments) {
        val text = getTextComponentForLanguage(locale.getLanguage(), key, arguments);
        if (text.isPresent()) {
            return text;
        }

        for (String fallbackLanguageName : locale.getLanguage().getFallbackLanguages()) {
            val fallbackLanguage = triton.getLanguageManager().getLanguageByName(fallbackLanguageName);
            if (!fallbackLanguage.isPresent()) {
                continue;
            }
            val textFallback = getTextComponentForLanguage(fallbackLanguage.get(), key, arguments);
            if (textFallback.isPresent()) {
                return textFallback;
            }
        }

        return getTextComponentForLanguage(triton.getLanguageManager().getMainLanguage(), key, arguments);
    }

    private @NotNull Optional<Component> getTextComponentForLanguage(@NotNull Language language, @NotNull String key, @NotNull Component... arguments) {
        this.triton.getLogger().logTrace("Trying to get translation with key '%1' in language '%2'", key, language.getLanguageId());
        val langItems = this.textItems.get(language);
        if (langItems == null) {
            return Optional.empty();
        }

        val msg = langItems.get(key);
        if (msg == null) {
            return Optional.empty();
        }

        this.triton.getLogger().logTrace("Found translation with key '%1' in language '%2'", key, language.getLanguageId());
        return Optional.of(replaceArguments(handleTranslationType(msg), arguments));
    }

    private Component handleTranslationType(String message) {
        // TODO make minimsg the default (?)
        if (message.startsWith(MINIMESSAGE_TYPE_TAG)) {
            return MiniMessage.miniMessage().deserialize(message.substring(MINIMESSAGE_TYPE_TAG.length()));
        } else if (message.startsWith(JSON_TYPE_TAG)) {
            return GsonComponentSerializer.gson().deserialize(message.substring(JSON_TYPE_TAG.length()));
        } else {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }
    }

    public @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location) {
        return getSignComponents(locale, location, () -> new Component[4]);
    }

    public @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale,
                                                            @NotNull SignLocation location,
                                                            @NotNull Component[] defaultLines) {
        return getSignComponents(locale, location, () -> defaultLines);
    }

    public @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale,
                                                            @NotNull SignLocation location,
                                                            @NotNull Supplier<Component[]> defaultLinesSupplier) {
        val lines = getSignComponentsForLanguage(locale.getLanguage(), location, defaultLinesSupplier);
        if (lines.isPresent()) {
            return lines;
        }

        for (String fallbackLanguageName : locale.getLanguage().getFallbackLanguages()) {
            val fallbackLanguage = triton.getLanguageManager().getLanguageByName(fallbackLanguageName);
            if (!fallbackLanguage.isPresent()) {
                continue;
            }
            val textFallback = getSignComponentsForLanguage(fallbackLanguage.get(), location, defaultLinesSupplier);
            if (textFallback.isPresent()) {
                return textFallback;
            }
        }

        return getSignComponentsForLanguage(triton.getLanguageManager().getMainLanguage(), location, defaultLinesSupplier);
    }

    private @NotNull Optional<Component[]> getSignComponentsForLanguage(@NotNull Language language,
                                                                        @NotNull SignLocation location,
                                                                        @NotNull Supplier<Component[]> defaultLinesSupplier) {
        this.triton.getLogger().logTrace("Trying to get sign translation on location '%1' in language '%2'", location, language.getLanguageId());
        val signTranslations = this.signItems.get(language);
        if (signTranslations == null) {
            return Optional.empty();
        }

        val lines = signTranslations.get(location);
        if (lines == null) {
            return Optional.empty();
        }

        this.triton.getLogger().logTrace("Found sign translation on location '%1' in language '%2'", location, language.getLanguageId());
        return Optional.of(formatLines(language, lines, defaultLinesSupplier));
    }

    public Component[] formatLines(@NonNull Language language,
                                   @NonNull String[] lines,
                                   @NonNull Supplier<@Nullable Component @NotNull []> defaultLinesSupplier) {
        val result = new Component[4];
        Component[] defaultLines = null;

        for (int i = 0; i < 4; ++i) {
            if (lines.length - 1 < i) {
                result[i] = Component.empty();
                continue;
            }
            if (!lines[i].equals("%use_line_default%")) {
                result[i] = handleTranslationType(lines[i]);
                continue;
            }

            // lazy load defaultLines
            if (defaultLines == null) {
                defaultLines = defaultLinesSupplier.get();
            }

            if (i >= defaultLines.length || defaultLines[i] == null) {
                result[i] = Component.empty();
                continue;
            }

            val currentIndex = i; // necessary for lambda function
            val currentDefaultLines = defaultLines; // necessary for lambda function
            this.triton.getMessageParser()
                    .translateComponent(defaultLines[i], language, this.triton.getConfig().getSignsSyntax())
                    .ifChanged(component -> result[currentIndex] = component)
                    .ifUnchanged(() -> result[currentIndex] = currentDefaultLines[currentIndex])
                    .ifToRemove(() -> result[currentIndex] = Component.empty());
        }

        return result;
    }

    public String matchPattern(String input, Localized localized) {
        return matchPattern(input, localized.getLanguageId());
    }

    public String matchPattern(String input, String language) {
        // TODO this needs to be rethought to work with Adventure
        for (Map.Entry<Pattern, LanguageText> entry : matches.entrySet()) {
            String replacement = entry.getValue().getMessageRegex(language);
            if (replacement == null) {
                replacement = entry.getValue().getMessageRegex(triton.getLanguageManager().getMainLanguage().getName());
            }
            if (replacement == null) {
                continue;
            }
            try {
                Matcher matcher = entry.getKey().matcher(input);
                input = matcher.replaceAll(replacement);
            } catch (IndexOutOfBoundsException e) {
                this.triton.getLogger().logError(
                        "Failed to translate using patterns: translation has more placeholders than regex groups. Translation key: %1",
                        entry.getValue().getKey());
            }
        }
        return input;
    }

    /**
     * @see com.rexcantor64.triton.language.parser.AdventureParser#replaceArguments(Component, List)
     */
    private Component replaceArguments(Component component, Component... args) {
        return triton.getMessageParser().replaceArguments(component, Arrays.asList(args));
    }

    private Component getTranslationNotFoundComponent(String key, Component... arguments) {
        val argumentsComponents = Component.join(JoinConfiguration.arrayLike(), arguments);

        return replaceArguments(translationNotFoundComponent, Component.text(key), argumentsComponents);
    }

    public int getTranslationCount() {
        return this.getTextTranslationCount() + this.getSignTranslationCount();
    }
}
