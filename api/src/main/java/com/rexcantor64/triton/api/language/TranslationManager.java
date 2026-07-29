package com.rexcantor64.triton.api.language;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Handles getting {@link Component Components} from the translations in a given language.
 *
 * @since 4.0.0
 */
public interface TranslationManager {

    /**
     * Get a text translation with a given key in the given language, replacing argument placeholders (%1, %2, etc.)
     * with the given arguments.
     * If the translation cannot be found, the 404 message is returned instead.
     *
     * @param locale    The language to get this translation in.
     * @param key       The key of the translation to get.
     * @param arguments The arguments to replace in the translation.
     * @return An Adventure component of the translation with its arguments replaced, or the 404 message if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Component getTextComponentOr404(@NotNull Localized locale, @NotNull String key, @NotNull Component... arguments);

    /**
     * Get a text translation with a given key in the given language, replacing argument placeholders (%1, %2, etc.)
     * with the given arguments.
     * If the translation cannot be found, an empty optional is returned instead.
     *
     * @param locale    The language to get this translation in.
     * @param key       The key of the translation to get.
     * @param arguments The arguments to replace in the translation.
     * @return An Adventure component of the translation with its arguments replaced, or an empty optional if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Optional<Component> getTextComponent(@NotNull Localized locale, @NotNull String key, @NotNull Component... arguments);

    /**
     * Get a text translation with a given key in the given language.
     * No arguments are automatically replaced, and it is responsibility of the caller to handle any replacement that may be needed.
     * Additionally, this returns the raw string in the configuration, which may include modifiers such as `[minimsg]`.
     * If the translation cannot be found, an empty optional is returned instead.
     *
     * @param locale The language to get this translation in.
     * @param key    The key of the translation to get.
     * @return The raw translation from the configuration, or an empty optional if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Optional<String> getTextString(@NotNull Localized locale, @NotNull String key);

    /**
     * Get a sign translation of a given location in the given language.
     * If the translation cannot be found, an empty optional is returned instead.
     *
     * @param locale   The language to get this translation in.
     * @param location The location of the sign.
     * @return An array with size 8 of Adventure components,
     * each representing a line of the sign (front and back) and containing the translation,
     * or an empty optional if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location);

    /**
     * Get a sign translation of a given location in the given language.
     * Additionally, all lines of the translation that match `%use_line_default%`,
     * will be replaced with the given default lines.
     * If the translation cannot be found, an empty optional is returned instead.
     *
     * @param locale       The language to get this translation in.
     * @param location     The location of the sign.
     * @param defaultLines An array with size 8 of Adventure components,
     *                     each representing a line of the sign (front and back)
     *                     and containing the text to replace all instances of `%use_line_default%` with.
     * @return An array with size 8 of Adventure components,
     * each representing a line of the sign (front and back) and containing the translation,
     * or an empty optional if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location, @NotNull Component[] defaultLines);

    /**
     * Get a sign translation of a given location in the given language.
     * Additionally, all lines of the translation that match `%use_line_default%`,
     * will be replaced with the given default lines.
     * If the translation cannot be found, an empty optional is returned instead.
     *
     * @param locale               The language to get this translation in.
     * @param location             The location of the sign.
     * @param defaultLinesSupplier A supplier that returns an array with size 8 of Adventure components,
     *                             each representing a line of the sign (front and back)
     *                             and containing the text to replace all instances of `%use_line_default%` with.
     * @return An array with size 8 of Adventure components,
     * each representing a line of the sign (front and back) and containing the translation,
     * or an empty optional if the translation was not found.
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location, @NotNull Supplier<Component[]> defaultLinesSupplier);

}
