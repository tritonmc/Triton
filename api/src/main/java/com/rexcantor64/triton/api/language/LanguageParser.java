package com.rexcantor64.triton.api.language;

import com.rexcantor64.triton.api.Triton;
import com.rexcantor64.triton.api.TritonAPI;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.val;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * The class responsible by translating messages with placeholders
 *
 * @deprecated Since 4.0.0. Use {@link MessageParser} instead.
 */
public interface LanguageParser {

    /**
     * Parses Triton's placeholders in a string and returns the result
     *
     * @param language The {@link Language#getName() name of the language} to use. If invalid, this will fallback
     *                 to the main language without warning.
     * @param syntax   The {@link FeatureSyntax} that'll be used for Triton's placeholders syntax.
     * @param input    The input {@link String}.
     * @return The input but with Triton's placeholders replaced by the message in the provided language.
     * @deprecated See class deprecation.
     */
    String parseString(String language, FeatureSyntax syntax, String input);

    /**
     * Parses Triton's placeholders in a {@link BaseComponent} array and returns the result
     *
     * @param language The {@link Language#getName() name of the language} to use. If invalid, this will fallback
     *                 to the main language without warning.
     * @param syntax   The {@link FeatureSyntax} that'll be used for Triton's placeholders syntax.
     * @param input    The input {@link BaseComponent}.
     * @return The input but with Triton's placeholders replaced by the message in the provided language.
     * @deprecated See class deprecation.
     */
    default BaseComponent[] parseComponent(String language, FeatureSyntax syntax, BaseComponent... input) {
        // hacky way to keep compatibility
        val result = TritonAPI.getInstance().getMessageParser()
                .translateComponent(
                        BungeeComponentSerializer.get().deserialize(input),
                        () -> TritonAPI.getInstance().getLanguageManager().getLanguageByNameOrDefault(language),
                        syntax
                );

        return result.getChanged()
                .map(component -> BungeeComponentSerializer.get().serialize(component))
                .orElseGet(() -> {
                    if (result.getState() == TranslationResult.ResultState.UNCHANGED) {
                        return input;
                    }
                    return null;
                });
    }

}
