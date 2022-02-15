package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdventureParserTest {

    private AdventureParser parser = new AdventureParser();
    private FeatureSyntax defaultSyntax = new MainConfig.FeatureSyntax("lang", "args", "arg");

    @Test
    public void testComponentWithoutFormatting() {
        Component comp = Component.text("Text [lang]translation.key[/lang] more text");

        Component result = parser.parseComponent("en_GB", defaultSyntax, comp);

        Component expected = Component.text("Text replaced placeholder more text");

        assertEquals(expected, result);
    }

}
