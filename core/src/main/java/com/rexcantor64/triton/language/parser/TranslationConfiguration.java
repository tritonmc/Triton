package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.Data;
import net.kyori.adventure.text.Component;

import java.util.function.BiFunction;

@Data
public class TranslationConfiguration {
    final FeatureSyntax featureSyntax;
    final String disabledLine;
    final BiFunction<String, Component[], Component> translationSupplier;
}
