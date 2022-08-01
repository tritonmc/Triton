package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import lombok.Data;

@Data
public class TranslationConfiguration {
    final FeatureSyntax featureSyntax;
    final Localized targetLocale;
}
