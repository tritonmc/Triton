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
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Component getTextComponentOr404(@NotNull Localized locale, @NotNull String key, Component... arguments);

    /**
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Optional<Component> getTextComponent(@NotNull Localized locale, @NotNull String key, @NotNull Component... arguments);

    /**
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Optional<String> getTextString(@NotNull Localized locale, @NotNull String key);

    /**
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location);

    /**
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location, @NotNull Component[] defaultLines);

    /**
     * // TODO javadoc
     *
     * @since 4.0.0
     */
    @NotNull Optional<Component[]> getSignComponents(@NotNull Localized locale, @NotNull SignLocation location, @NotNull Supplier<Component[]> defaultLinesSupplier);

}
