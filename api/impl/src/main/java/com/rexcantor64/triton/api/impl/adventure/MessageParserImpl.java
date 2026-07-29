package com.rexcantor64.triton.api.impl.adventure;


import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.impl.ComponentUtils;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.api.language.TranslationResult;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class MessageParserImpl implements MessageParser {

    private final com.rexcantor64.triton.language.parser.MessageParser parser;

    @Override
    public @NotNull TranslationResult<String> translateString(@NotNull String text, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        return parser.translateString(text, language, syntax);
    }

    @Override
    public @NotNull TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        return parser.translateComponentJson(ComponentUtils.serializeToJsonTree(component), language, syntax)
                .map(ComponentUtils::deserializeFromJsonTree);
    }
}
