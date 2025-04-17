package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class MessageParser implements com.rexcantor64.triton.api.language.MessageParser {

    /**
     * Given a Component with "%1", "%2", etc., replace these with the given arguments.
     *
     * @param component The component with % placeholders.
     * @param arguments The list of arguments available to use as replacements.
     * @return The component with % placeholders replaced with arguments.
     * @since 4.0.0
     */
    public abstract @NotNull Component replaceArguments(@NotNull Component component, @NotNull List<@NotNull Component> arguments);

    /**
     * See {@link com.rexcantor64.triton.api.language.MessageParser#translateString(String, Localized, FeatureSyntax)}
     */
    // Method has to be redeclared so that it doesn't break with jar-in-jar relocation
    @Override
    public abstract @NotNull TranslationResult<String> translateString(@NotNull String text, @NotNull Localized language, @NotNull FeatureSyntax syntax);

    /**
     * See {@link com.rexcantor64.triton.api.language.MessageParser#translateComponent(Component, Localized, FeatureSyntax)}
     */
    // Method has to be redeclared so that it doesn't break with jar-in-jar relocation
    @Override
    public abstract @NotNull TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull Localized language, @NotNull FeatureSyntax syntax);

}
