package com.rexcantor64.triton.api.legacy;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.language.localized.StringLocale;

import java.util.function.Function;

public class LegacyLanguageParser implements LanguageParser {

    @Override
    @Deprecated
    public String parseString(String language, FeatureSyntax syntax, String input) {
        return Triton.get().getMessageParser()
                .translateString(input, new StringLocale(language), syntax)
                .mapToObj(
                        Function.identity(),
                        () -> input,
                        () -> null
                );
    }
}
