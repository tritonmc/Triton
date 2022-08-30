package com.rexcantor64.triton.api.legacy;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.language.localized.StringLocale;
import com.rexcantor64.triton.language.parser.TranslationResult;
import lombok.val;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class LegacyLanguageParser implements LanguageParser {

    @Override
    public String parseString(String language, FeatureSyntax syntax, String input) {
        val result = Triton.get().getAdventureParser()
                .translateComponent(
                        LegacyComponentSerializer.legacySection().deserialize(input),
                        new StringLocale(language),
                        syntax
                );
        if (result.getState() == TranslationResult.ResultState.CHANGED) {
            return LegacyComponentSerializer.legacySection().serialize(result.getResult());
        } else if (result.getState() == TranslationResult.ResultState.UNCHANGED) {
            return input;
        }
        return null;
    }
}
