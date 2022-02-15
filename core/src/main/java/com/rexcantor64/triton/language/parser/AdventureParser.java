package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

public class AdventureParser {

    public Component parseComponent(String language, FeatureSyntax syntax, Component component) {
        // TODO
        TextReplacementConfig replacementConfig = TextReplacementConfig.builder()
                .matchLiteral("[lang]translation.key[/lang]")
                .replacement("replaced placeholder")
                .build();
        return component.replaceText(replacementConfig);
    }

}
