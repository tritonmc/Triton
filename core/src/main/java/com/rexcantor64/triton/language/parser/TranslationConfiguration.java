package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import lombok.Data;
import net.kyori.adventure.text.Component;

import java.util.function.Function;

@Data
public class TranslationConfiguration {
    final FeatureSyntax featureSyntax;
    final Function<String, Component> translationSupplier;
}
