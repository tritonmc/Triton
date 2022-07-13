package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.localized.StringLocale;
import com.rexcantor64.triton.language.parser.AdventureParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdventureParserTest {

    private final AdventureParser parser = new AdventureParser();
    private final FeatureSyntax defaultSyntax = new MainConfig.FeatureSyntax("lang", "args", "arg");

    @Test
    public void testComponentWithoutFormatting() {
        Component comp = Component.text().color(TextColor.color(0x0000ff)).append(
                Component.text().color(TextColor.color(0xff000)).content("Text [lang]translation."),
                Component.text().content("key[/lang][lang]test[/lang] more text"),
                Component.text().color(TextColor.color(0x00ff00)).content(" and this doesn't have placeholders")
        ).asComponent();

        //Component comp = Component.text("Text [lang]translation.key[/lang] more text");

        Component result = parser.parseComponent(new StringLocale("en_GB"), defaultSyntax, comp);

        Component expected = Component.text("Text replaced placeholder more text");

        assertEquals(expected, result);
    }

}
