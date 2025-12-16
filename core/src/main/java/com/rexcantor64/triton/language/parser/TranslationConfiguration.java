package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

@Data
public class TranslationConfiguration<T> {
    final FeatureSyntax featureSyntax;
    final String disabledLine;
    final BiFunction<String, T[], T> translationSupplier;
    /**
     * For legacy parser only.
     */
    final Function<@NotNull String, @NotNull String> matchSupplier;
}
