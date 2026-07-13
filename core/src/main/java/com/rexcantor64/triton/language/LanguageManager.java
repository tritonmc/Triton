package com.rexcantor64.triton.language;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.language.SignLocation;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.language.localized.StringLocale;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class LanguageManager implements com.rexcantor64.triton.api.language.LanguageManager {

    private final Triton<?, ?> triton;

    private List<Language> languages = new ArrayList<>();
    private Language mainLanguage;

    @Deprecated
    public String matchPattern(String input, LanguagePlayer p) {
        return triton.getTranslationManager().matchPattern(input, p);
    }

    @Deprecated
    public String getText(@NonNull LanguagePlayer p, String code, Object... args) {
        Component[] argsComponents = Arrays.stream(args)
                .map(Objects::toString)
                .map(arg -> LegacyComponentSerializer.legacyAmpersand().deserialize(arg))
                .toArray(Component[]::new);
        val resultComponent = triton.getTranslationManager().getTextComponentOr404(p, code, argsComponents);
        return ComponentUtils.serializeToLegacy(resultComponent);
    }

    @Deprecated
    public String getText(@NonNull String languageName, @NonNull String code, @NonNull Object... args) {
        Component[] argsComponents = Arrays.stream(args)
                .map(Objects::toString)
                .map(arg -> LegacyComponentSerializer.legacyAmpersand().deserialize(arg))
                .toArray(Component[]::new);
        val resultComponent = triton.getTranslationManager().getTextComponentOr404(new StringLocale(languageName), code, argsComponents);
        return ComponentUtils.serializeToLegacy(resultComponent);
    }

    @Deprecated
    public String getTextFromMain(@NonNull String code, @NonNull Object... args) {
        return getText(this.getMainLanguage().getName(), code, args);
    }

    @Deprecated
    public String[] getSign(LanguagePlayer player, SignLocation location) {
        return getSign(player, location, () -> new String[8]);
    }

    @Deprecated
    public String[] getSign(@NonNull LanguagePlayer player, SignLocation location, String[] defaultLines) {
        return getSign(player.getLang().getName(), location, () -> defaultLines);
    }

    @Deprecated
    public String[] getSign(@NonNull LanguagePlayer player, SignLocation location, Supplier<String[]> defaultLines) {
        return getSign(player.getLang().getName(), location, defaultLines);
    }

    @Deprecated
    public String[] getSign(@NonNull String language, @NonNull SignLocation location,
                            @NonNull Supplier<String[]> defaultLines) {
        val signComponents = triton.getTranslationManager()
                .getSignComponents(
                        new StringLocale(language),
                        location,
                        () -> Arrays.stream(defaultLines.get())
                                .map(ComponentUtils::deserializeFromLegacy)
                                .toArray(Component[]::new)
                );

        return signComponents.map(components ->
                        Arrays.stream(components)
                                .map(ComponentUtils::serializeToLegacy)
                                .toArray(String[]::new)
                )
                .orElse(null);

    }

    @Override
    public @NotNull Optional<Language> getLanguageByName(@NotNull String name) {
        return this.languages.stream()
                .filter(language -> Objects.equals(language.getName(), name))
                .findFirst();
    }

    @Override
    public @NotNull Language getLanguageByNameOrDefault(@NotNull String name) {
        return this.getLanguageByName(name).orElse(this.mainLanguage);
    }

    @Override
    public @NotNull Optional<Language> getLanguageByLocale(@NotNull String locale) {
        return this.languages.stream()
                .filter(language ->
                        language.getMinecraftCodes()
                                .stream()
                                .anyMatch(languageLocale -> languageLocale.equalsIgnoreCase(locale))
                )
                .findFirst();
    }

    @Override
    public com.rexcantor64.triton.api.language.@NotNull Language getLanguageByLocaleOrDefault(@NotNull String locale) {
        return this.getLanguageByLocale(locale).orElse(this.mainLanguage);
    }

    @Deprecated
    public Language getLanguageByName(String name, boolean fallback) {
        if (name != null)
            for (Language lang : languages)
                if (lang.getName().equals(name))
                    return lang;
        if (fallback) return mainLanguage;
        return null;
    }

    @Deprecated
    public Language getLanguageByLocale(String name, boolean fallback) {
        if (name != null)
            for (Language lang : languages)
                for (String s : lang.getMinecraftCodes())
                    if (s.equalsIgnoreCase(name))
                        return lang;
        if (fallback) return mainLanguage;
        return null;
    }

    public List<Language> getAllLanguages() {
        return this.languages;
    }

    public Language getMainLanguage() {
        return this.mainLanguage;
    }

    public void setup() {
        this.triton.getLogger().logDebug("Setting up language manager...");

        val languages = this.triton.getConfig().getLanguages();
        val mainLang = this.triton.getConfig().getMainLanguage();

        for (val lang : languages) {
            if (lang.getName().equals(mainLang)) {
                this.mainLanguage = lang;
            }
        }
        if (this.mainLanguage == null) {
            this.mainLanguage = languages.get(0);
            this.triton.getLogger().logWarning(
                    "The main language on config did not match any known language. Using %1 as the main language instead",
                    this.mainLanguage.getName()
            );
        }

        this.languages = Collections.unmodifiableList(languages);

        this.triton.getLogger()
                .logInfo("Successfully setup the Language Manager! %1 languages loaded!",
                        this.languages.size());
    }

}
