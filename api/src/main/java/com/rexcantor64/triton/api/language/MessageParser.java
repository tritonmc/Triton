package com.rexcantor64.triton.api.language;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import net.kyori.adventure.text.Component;

/**
 * Replacement for {@link LanguageParser} that uses the Adventure chat library
 * instead of md_5's chat library;
 */
public interface MessageParser {

    /**
     * Find and replace Triton placeholders in a Component.
     * <p>
     * A translation can yield three states:
     * <ul>
     *     <li>placeholders are found and therefore translated;</li>
     *     <li>placeholders aren't found and therefore the component is left unchanged;</li>
     *     <li>a "disabled line" placeholder is found.</li>
     * </ul>
     * See {@link TranslationResult} for more details.
     *
     * @param component The component to find and replace Triton placeholders on.
     * @param language  The language to fetch translations on.
     * @param syntax    The syntax to use while searching for Triton placeholders.
     * @return The result of the translation
     * @since 4.0.0
     */
    TranslationResult translateComponent(Component component, Localized language, FeatureSyntax syntax);

}
