package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.Data;

import java.util.function.BiFunction;

@Data
public class TranslationConfiguration<T> {
    final FeatureSyntax featureSyntax;
    final String disabledLine;
    final BiFunction<String, T[], T> translationSupplier;
}
