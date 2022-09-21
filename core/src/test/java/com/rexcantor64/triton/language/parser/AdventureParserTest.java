package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.utils.DefaultFeatureSyntax;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        return Component.text("unknown placeholder");
    };

    private final TranslationConfiguration configuration = new TranslationConfiguration(
            defaultSyntax,
            "disabled.line",
            (key, args) -> parser.replaceArguments(messageResolver.apply(key), Arrays.asList(args))
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
                                                                .content(" ")
                                                                .color(NamedTextColor.BLUE)
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
    public void testSplitComponentWithoutStyles() {
        Component toSplit = Component.text("Test splitting a component without styles");
        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{0, 12, 36})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text(""),
                Component.text("Test splitti"),
                Component.text("ng a component without s"),
                Component.text("tyles")
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithStyles() {
        // |Lorem i|psum dol|or sit amet, consectetur adipi|scing eli|t. Cras tincidunt ligula vel an|te laoreet tempor.
        Component toSplit = Component.text()
                .content("Lorem ipsum ")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .append(
                        Component.text("dolor sit amet, consectetur")
                                .color(TextColor.color(0x123456))
                                .append(
                                        Component.text(" adipiscing").decorate(TextDecoration.ITALIC)
                                ),
                        Component.text(" elit. ")
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com")),
                        Component.text("Cras tincidunt ligula vel ante laoreet tempor.")
                                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{0, 7, 15, 45, 54, 85})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text("")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD),
                Component.text("Lorem i").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("psum ")
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(Component.text("dol").color(TextColor.color(0x123456))),
                Component.text()
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(
                                Component.text("or sit amet, consectetur")
                                        .color(TextColor.color(0x123456))
                                        .append(
                                                Component.text(" adipi").decorate(TextDecoration.ITALIC)
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(
                                Component.text()
                                        .color(TextColor.color(0x123456))
                                        .append(
                                                Component.text("scing").decorate(TextDecoration.ITALIC)
                                        ),
                                Component.text(" eli")
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com"))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(
                                Component.text("t. ")
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com")),
                                Component.text("Cras tincidunt ligula vel an")
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.RED)
                        .decorate(TextDecoration.BOLD)
                        .append(
                                Component.text("te laoreet tempor.")
                                        .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                        )
                        .asComponent()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithDeepTree() {
        // Lorem ipsum do|lor s|it am|et, consec|tetur adipi|scing e|lit
        Component toSplit = Component.text()
                .content("Lorem ipsum ")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.STRIKETHROUGH)
                .append(
                        Component.text()
                                .decorate(TextDecoration.ITALIC)
                                .append(
                                        Component.text()
                                                .content("dolor ")
                                                .append(
                                                        Component.text("sit ")
                                                                .color(TextColor.color(0xabc123)),
                                                        Component.text("amet")
                                                                .decorate(TextDecoration.UNDERLINED)
                                                                .insertion("insertion text")
                                                                .hoverEvent(HoverEvent.showText(Component.text("hover text")))
                                                ),
                                        Component.text(", consectetur ")
                                                .decorate(TextDecoration.BOLD)
                                ),
                        Component.text()
                                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com"))
                                .append(
                                        Component.text()
                                                .content("adipiscing elit")
                                                .color(TextColor.color(0x987654))
                                                .decorate(TextDecoration.OBFUSCATED)
                                )
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{14, 19, 24, 34, 45, 52})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text()
                        .content("Lorem ipsum ")
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .decorate(TextDecoration.ITALIC)
                                        .append(
                                                Component.text().content("do")
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .decorate(TextDecoration.ITALIC)
                                        .append(
                                                Component.text()
                                                        .content("lor ")
                                                        .append(
                                                                Component.text("s")
                                                                        .color(TextColor.color(0xabc123))
                                                        )
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .decorate(TextDecoration.ITALIC)
                                        .append(
                                                Component.text()
                                                        .append(
                                                                Component.text("it ")
                                                                        .color(TextColor.color(0xabc123)),
                                                                Component.text("am")
                                                                        .decorate(TextDecoration.UNDERLINED)
                                                                        .insertion("insertion text")
                                                                        .hoverEvent(HoverEvent.showText(Component.text("hover text")))
                                                        )
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .decorate(TextDecoration.ITALIC)
                                        .append(
                                                Component.text()
                                                        .append(
                                                                Component.text("et")
                                                                        .decorate(TextDecoration.UNDERLINED)
                                                                        .insertion("insertion text")
                                                                        .hoverEvent(HoverEvent.showText(Component.text("hover text")))
                                                        ),
                                                Component.text(", consec")
                                                        .decorate(TextDecoration.BOLD)
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .decorate(TextDecoration.ITALIC)
                                        .append(
                                                Component.text("tetur ")
                                                        .decorate(TextDecoration.BOLD)
                                        ),
                                Component.text()
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com"))
                                        .append(
                                                Component.text()
                                                        .content("adipi")
                                                        .color(TextColor.color(0x987654))
                                                        .decorate(TextDecoration.OBFUSCATED)
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com"))
                                        .append(
                                                Component.text()
                                                        .content("scing e")
                                                        .color(TextColor.color(0x987654))
                                                        .decorate(TextDecoration.OBFUSCATED)
                                        )
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .decorate(TextDecoration.STRIKETHROUGH)
                        .append(
                                Component.text()
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://triton.rexcantor64.com"))
                                        .append(
                                                Component.text()
                                                        .content("lit")
                                                        .color(TextColor.color(0x987654))
                                                        .decorate(TextDecoration.OBFUSCATED)
                                        )
                        )
                        .asComponent()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentOnBorder() {
        // |Lorem ipsum |dolor sit amet, |consectetur adipiscing elit|
        Component toSplit = Component.text()
                .content("Lorem ipsum ")
                .color(NamedTextColor.GREEN)
                .append(
                        Component.text("dolor sit amet, ")
                                .decorate(TextDecoration.ITALIC),
                        Component.text("consectetur adipiscing elit")
                                .color(TextColor.color(0x123456))
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{0, 12, 28, 55})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .asComponent(),
                Component.text()
                        .content("Lorem ipsum ")
                        .color(NamedTextColor.GREEN)
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text("dolor sit amet, ")
                                        .decorate(TextDecoration.ITALIC)
                        )
                        .asComponent(),
                Component.text()
                        .content("")
                        .color(NamedTextColor.GREEN)
                        .append(
                                // FIXME I don't think this first component should be required for it to pass
                                Component.text()
                                        .decorate(TextDecoration.ITALIC),
                                Component.text("consectetur adipiscing elit")
                                        .color(TextColor.color(0x123456))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .asComponent(),
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithNonTextComponents() {
        // Lorem ipsum |Xdol|or sit amet, X|consectetur adip|iscing elit
        Component toSplit = Component.text()
                .content("Lorem ipsum ")
                .color(NamedTextColor.GREEN)
                .append(
                        Component.translatable("test translatable")
                                .append(Component.text("dolor ")),
                        Component.text("sit amet, ")
                                .decorate(TextDecoration.ITALIC),
                        Component.keybind("ALT"),
                        Component.text("consectetur adipiscing elit")
                                .color(TextColor.color(0x123456))
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{12, 16, 30, 46})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text()
                        .content("Lorem ipsum ")
                        .color(NamedTextColor.GREEN)
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.translatable("test translatable")
                                        .append(Component.text("dol"))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text()
                                        .append(Component.text("or ")),
                                Component.text("sit amet, ")
                                        .decorate(TextDecoration.ITALIC),
                                Component.keybind("ALT")
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text("consectetur adip")
                                        .color(TextColor.color(0x123456))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text("iscing elit")
                                        .color(TextColor.color(0x123456))
                        )
                        .asComponent()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithNonTextComponentsWithRepeatedIndexes() {
        // Lorem ipsum |||Xdol|or sit amet, X|||consectetur adip|iscing elit
        Component toSplit = Component.text()
                .content("Lorem ipsum ")
                .color(NamedTextColor.GREEN)
                .append(
                        Component.translatable("test translatable")
                                .append(Component.text("dolor ")),
                        Component.text("sit amet, ")
                                .decorate(TextDecoration.ITALIC),
                        Component.keybind("ALT"),
                        Component.text("consectetur adipiscing elit")
                                .color(TextColor.color(0x123456))
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{12, 12, 12, 16, 30, 30, 30, 46})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.text()
                        .content("Lorem ipsum ")
                        .color(NamedTextColor.GREEN)
                        .asComponent(),
                Component.empty(),
                Component.empty(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.translatable("test translatable")
                                        .append(Component.text("dol"))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text()
                                        .append(Component.text("or ")),
                                Component.text("sit amet, ")
                                        .decorate(TextDecoration.ITALIC),
                                Component.keybind("ALT")
                        )
                        .asComponent(),
                Component.empty(),
                Component.empty(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text("consectetur adip")
                                        .color(TextColor.color(0x123456))
                        )
                        .asComponent(),
                Component.text()
                        .color(NamedTextColor.GREEN)
                        .append(
                                Component.text("iscing elit")
                                        .color(TextColor.color(0x123456))
                        )
                        .asComponent()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithSequentialNonTextComponents() {
        // X|X
        Component toSplit = Component.text()
                .append(
                        Component.keybind("CTRL"),
                        Component.keybind("ALT")
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{1})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.keybind("CTRL"),
                Component.keybind("ALT")
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithSequentialNonTextComponentsWithRepeatedIndexes() {
        // |X||X|
        Component toSplit = Component.text()
                .append(
                        Component.keybind("CTRL"),
                        Component.keybind("ALT")
                )
                .asComponent();

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{0, 1, 1, 2})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.empty(),
                Component.keybind("CTRL"),
                Component.empty(),
                Component.keybind("ALT"),
                Component.empty()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitComponentWithSingleNonTextComponentWithRepeatedIndexes() {
        // ||X||
        Component toSplit = Component.keybind("CTRL");

        Queue<Integer> splitIndexes = Arrays.stream(new Integer[]{0, 0, 1, 1})
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> result = parser.splitComponent(toSplit, splitIndexes);

        Component[] expected = new Component[]{
                Component.empty(),
                Component.empty(),
                Component.keybind("CTRL"),
                Component.empty(),
                Component.empty()
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testGetPatternIndexArray() {
        String input = "Lorem ipsum [tag]dolor [tag]sit[/tag] amet[/tag], [tag2]consectetur[/tag2] [tag]adipiscing elit[/tag]. Nullam posuere.";

        List<Integer[]> result = parser.getPatternIndexArray(input, "tag");

        List<Integer[]> expected = Arrays.asList(
                new Integer[]{12, 48, 17, 42},
                new Integer[]{75, 101, 80, 95}
        );

        assertEquals(expected.size(), result.size());
        for (int i = 0; i < expected.size(); i++) {
            assertArrayEquals(expected.get(i), result.get(i));
        }
    }

}
