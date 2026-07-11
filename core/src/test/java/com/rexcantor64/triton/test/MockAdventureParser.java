package com.rexcantor64.triton.test;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.language.parser.TranslationConfiguration;
import com.rexcantor64.triton.language.parser.TranslationResult;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MockAdventureParser extends AdventureParser {

    @Override
    public @NotNull TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        val configuration = new TranslationConfiguration<Component>(
                syntax,
                "disabled.line",
                (key, args) -> Component.text("replaced(" + key + ")"),
                Function.identity()
        );
        return translateComponent(component, configuration);
    }
}
