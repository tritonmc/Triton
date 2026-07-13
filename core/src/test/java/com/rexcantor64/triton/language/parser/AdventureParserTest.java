package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.test.DefaultFeatureSyntax;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static com.rexcantor64.triton.utils.ComponentUtils.SECTION_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdventureParserTest {

    private final AdventureParser parser = new AdventureParser();
    private final FeatureSyntax defaultSyntax = new DefaultFeatureSyntax();

    private final Function<String, Component> messageResolver = (key) -> {
        if (key.equals("without.formatting")) {
            return Component.text("This is text without formatting");
        }
        if (key.equals("without.formatting.with.args")) {
            return Component.text("This is text without formatting but with arguments (%1)");
        }
        if (key.equals("with.colors")) {
            return Component.text("This text is green").color(NamedTextColor.GREEN);
        }
        if (key.equals("with.colors.two.args")) {
            return Component.text("This text is pink and has two arguments (%1 and %2)")
                    .color(NamedTextColor.LIGHT_PURPLE);
        }
        if (key.equals("with.colors.repeated.args")) {
            return Component.text("This text is pink and has three arguments (%1 and %2 and %1)")
                    .color(NamedTextColor.LIGHT_PURPLE);
        }
        if (key.equals("nested")) {
            return Component.text()
                    .content("some text")
                    .append(Component.text("[lang]without.formatting[/lang]"))
                    .asComponent();
        }
        if (key.equals("with.placeholder.colors")) {
            return Component.text()
                    .append(
                            Component.text("%1 ").color(NamedTextColor.LIGHT_PURPLE),
                            Component.text("is a very cool guy").color(NamedTextColor.GREEN)
                    )
                    .asComponent();
        }
        if (key.equals("change.colors.on.args")) {
            return Component.text()
                    .append(
                            Component.text("Some text ").color(NamedTextColor.RED),
                            Component.text("%1 more text").color(NamedTextColor.BLUE)
                    )
                    .asComponent();
        }
        return Component.text("unknown placeholder");
    };

    private final TranslationConfiguration<Component> configuration = new TranslationConfiguration<>(
            defaultSyntax,
            "disabled.line",
            (key, args) -> parser.replaceArguments(messageResolver.apply(key), Arrays.asList(args)),
            Function.identity()
    );

    @Test
    public void testParseComponentWithoutPlaceholders() {
        Component comp = Component.text("Text without any placeholders whatsoever");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testParseComponentWithoutFormatting() {
        Component comp = Component.text("Text [lang]without.formatting[/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text("Text This is text without formatting more text");

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithSideBySideComponents() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text [lang]without."),
                        Component.text()
                                .content("formatting[/lang][lang]with.colors[/lang] more text"),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .content(" and this doesn't have placeholders")
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text This is text without formatting"),
                        Component.text()
                                .content("This text is green")
                                .color(NamedTextColor.GREEN),
                        Component.text()
                                .color(TextColor.color(0x0000ff))
                                .content(" more text")
                                .append(
                                        Component.text()
                                                .color(TextColor.color(0x00ff00))
                                                .content(" and this doesn't have placeholders")
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithOneArgument() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text [lang]without."),
                        Component.text("formatting.with.args[arg]test[/arg][/lang] more text")
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("Text This is text without formatting but with arguments (")
                                .color(TextColor.color(0xff0000))
                                .append(
                                        Component.text()
                                                .content("test")
                                                .color(TextColor.color(0x0000ff)),
                                        Component.text(")")
                                ),
                        Component.text()
                                .color(TextColor.color(0x0000ff))
                                .content(" more text")
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithTwoArguments() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .content("[lang]with.colors.two.args[arg]")
                                .append(
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text("[/arg][arg]"),
                                        Component.text()
                                                .content("second arg")
                                                .color(NamedTextColor.BLACK),
                                        Component.text("[/arg][/lang]")
                                )
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .append(
                                        Component.text()
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                                .content("This text is pink and has two arguments ("),
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text()
                                                .content(" and ")
                                                .color(NamedTextColor.LIGHT_PURPLE),
                                        Component.text()
                                                .content("second arg")
                                                .color(NamedTextColor.BLACK),
                                        Component.text()
                                                .content(")")
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithRepeatedArguments() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .content("[lang]with.colors.repeated.args[arg]")
                                .append(
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text("[/arg][arg]"),
                                        Component.text()
                                                .content("second arg")
                                                .color(NamedTextColor.BLACK),
                                        Component.text("[/arg][/lang]")
                                )
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .append(
                                        Component.text()
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                                .content("This text is pink and has three arguments ("),
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text()
                                                .content(" and ")
                                                .color(NamedTextColor.LIGHT_PURPLE),
                                        Component.text()
                                                .content("second arg")
                                                .color(NamedTextColor.BLACK),
                                        Component.text()
                                                .content(" and ")
                                                .color(NamedTextColor.LIGHT_PURPLE),
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text()
                                                .content(")")
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithFewerArgumentsThanExpected() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .content("[lang]with.colors.two.args[arg]")
                                .append(
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text("[/arg][/lang]")
                                )
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff0000))
                                .content("Text "),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .append(
                                        Component.text()
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                                .content("This text is pink and has two arguments ("),
                                        Component.text()
                                                .content("first arg")
                                                .color(NamedTextColor.AQUA),
                                        Component.text()
                                                .content(" and %2)")
                                                .color(NamedTextColor.LIGHT_PURPLE)
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentBackwardsCompatibilityWithArgsTag() {
        Component comp = Component.text("Text [lang]with.colors.two.args[args][arg]test[/arg][/args][/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text("Text "),
                        Component.text()
                                .color(NamedTextColor.LIGHT_PURPLE)
                                .content("This text is pink and has two arguments (test and %2)"),
                        Component.text(" more text")
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithNonTextComponents() {
        Component comp = Component.text()
                .content("Text ")
                .append(
                        Component.translatable("translatable.key"),
                        Component.text("[lang]without.formatting[/lang] more text")
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("Text ")
                                .append(
                                        Component.translatable("translatable.key")
                                ),
                        Component.text("This is text without formatting more text")
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithPlaceholdersOnlyInTranslatableComponentArguments() {
        Component comp = Component.translatable(
                "translatable.key",
                Component.text("[lang]without.formatting[/lang]")
        );

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.translatable(
                "translatable.key",
                Component.text()
                        .append(Component.text("This is text without formatting"))
        );

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithPlaceholdersInTranslatableComponentArguments() {
        Component comp = Component.text()
                .content("Text ")
                .append(
                        Component.translatable(
                                "translatable.key",
                                Component.text("[lang]without.formatting[/lang]")
                        ),
                        Component.text("[lang]without.formatting[/lang] more text")
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("Text ")
                                .append(
                                        Component.translatable(
                                                "translatable.key",
                                                Component.text()
                                                        .append(Component.text("This is text without formatting"))
                                        )
                                ),
                        Component.text("This is text without formatting more text")
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithPlaceholdersInShowTextHoverAction() {
        Component comp = Component.text()
                .content("some text")
                .hoverEvent(HoverEvent.showText(Component.text("[lang]without.formatting[/lang]")))
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .content("some text")
                .hoverEvent(
                        HoverEvent.showText(
                                Component.text()
                                        .append(Component.text("This is text without formatting"))
                        )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithPlaceholdersInShowEntityHoverAction() {
        Component comp = Component.text()
                .content("some text")
                .hoverEvent(
                        HoverEvent.showEntity(
                                Key.key("creeper"),
                                new UUID(0, 0),
                                Component.text("[lang]without.formatting[/lang]")
                        )
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .content("some text")
                .hoverEvent(
                        HoverEvent.showEntity(
                                Key.key("creeper"),
                                new UUID(0, 0),
                                Component.text()
                                        .append(Component.text("This is text without formatting"))
                                        .asComponent()
                        )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWhileRetainingCorrectStyles() {
        Component comp = Component.text()
                .content("[lang]with.placeholder.colors[arg]Rexcantor64[/arg][/lang]")
                .color(NamedTextColor.BLUE)
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("")
                                .color(NamedTextColor.BLUE), // hack because component compaction is buggy
                        Component.text()
                                .content("")
                                .color(NamedTextColor.BLUE)
                                .append(
                                        Component.text()
                                                .append(
                                                        Component.text("Rexcantor64")
                                                                .color(NamedTextColor.LIGHT_PURPLE),
                                                        Component.text()
                                                                .content("")
                                                                .color(NamedTextColor.BLUE)
                                                                .append(
                                                                        Component.text(" ")
                                                                                .color(NamedTextColor.LIGHT_PURPLE)
                                                                )
                                                                .append(
                                                                        Component.text("is a very cool guy")
                                                                                .color(NamedTextColor.GREEN)
                                                                )
                                                )
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWhileRetainingCorrectStylesWithLinkBug() {
        // server adds invalid link to text
        Component comp = Component.text()
                .append(
                        Component.text("[lang]")
                                .color(NamedTextColor.DARK_GRAY),
                        Component.text("change.colors.on.args[arg]5[/arg][/lang]")
                                .color(NamedTextColor.DARK_GRAY)
                                .clickEvent(ClickEvent.openUrl("http://change.colors.on.args[arg]5[/arg][/lang]"))
                )
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("")
                                .color(NamedTextColor.DARK_GRAY), // hack because component compaction is buggy
                        Component.text()
                                .content("")
                                .color(NamedTextColor.DARK_GRAY)
                                .clickEvent(ClickEvent.openUrl("http://change.colors.on.args[arg]5[/arg][/lang]"))
                                .append(
                                        Component.text()
                                                .append(
                                                        Component.text("Some text ").color(NamedTextColor.RED),
                                                        Component.text("5 more text").color(NamedTextColor.BLUE)
                                                )
                                                .asComponent()
                                )
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithNestedPlaceholders() {
        Component comp = Component.text("[lang]nested[/lang]");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .content("some text")
                .append(
                        Component.text("This is text without formatting"))
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentWithDisabledLine() {
        Component comp = Component.text("[lang]disabled.line[/lang]");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        assertEquals(TranslationResult.ResultState.TO_REMOVE, result.getState());
    }

    @Test
    public void testParseComponentWithLegacyColorCodesInsideJson() {
        Component comp = Component.text(SECTION_CHAR + "asomething " + SECTION_CHAR + "c[lang]without.formatting[/lang]");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("something ")
                                .color(NamedTextColor.GREEN),
                        Component.text("This is text without formatting")
                                .color(NamedTextColor.RED)
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testGetStyleOfFirstStyle() {
        Component comp = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("Lorem Ipsum").color(NamedTextColor.BLUE)
                )
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .asComponent();

        Optional<Style> result = parser.getStyleOfFirstCharacter(comp);

        Style expected = Style.style()
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD)
                .build();

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    public void testGetStyleOfFirstStyleEmpty() {
        Component comp = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.BLUE)
                )
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .asComponent();

        Optional<Style> result = parser.getStyleOfFirstCharacter(comp);

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetStyleOfFirstStyleWithEmptyTextComponent() {
        Component comp = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.RED),
                        Component.text("Lorem Ipsum").color(NamedTextColor.BLUE)
                )
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .asComponent();

        Optional<Style> result = parser.getStyleOfFirstCharacter(comp);

        Style expected = Style.style()
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.BOLD)
                .build();

        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    public void testStripStyleOfFirstCharacter() {
        Component comp = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.RED),
                        Component.text("Lorem Ipsum").color(NamedTextColor.BLUE)
                )
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .asComponent();

        Component result = parser.stripStyleOfFirstCharacter(comp);

        Component expected = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.RED),
                        Component.text("Lorem Ipsum")
                )
                .asComponent();

        assertEquals(expected, result);
    }

    @Test
    public void testStripStyleOfFirstCharacterApplyFallback() {
        Component comp = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.RED),
                        Component.text("Lorem Ipsum").color(NamedTextColor.BLUE),
                        Component.text(" dolor sit amet")
                )
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .asComponent();

        Component result = parser.stripStyleOfFirstCharacter(comp);

        Component expected = Component.text()
                .append(
                        Component.keybind("test"),
                        Component.text("").color(NamedTextColor.RED),
                        Component.text("Lorem Ipsum"),
                        Component.text(" dolor sit amet")
                                .color(NamedTextColor.LIGHT_PURPLE)
                                .decorate(TextDecoration.BOLD)
                )
                .asComponent();

        assertEquals(expected, result);
    }

}
