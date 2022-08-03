package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.utils.DefaultFeatureSyntax;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdventureParserTest {

    private final AdventureParser parser = new AdventureParser();
    private final FeatureSyntax defaultSyntax = new DefaultFeatureSyntax();

    private final TranslationConfiguration configuration = new TranslationConfiguration(
            defaultSyntax,
            (key) -> {
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
                    return Component.text("This text is pink and has two arguments (%1 and %2)").color(NamedTextColor.LIGHT_PURPLE);
                }
                return Component.text("unknown placeholder");
            }
    );

    @Test
    public void testParseComponentWithoutPlaceholders() {
        Component comp = Component.text("Text without any placeholders whatsoever");

        TranslationResult result = parser.parseComponent(comp, configuration);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testParseComponentWithoutFormatting() {
        Component comp = Component.text("Text [lang]without.formatting[/lang] more text");

        TranslationResult result = parser.parseComponent(comp, configuration);

        Component expected = Component.text("Text This is text without formatting more text");

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertEquals(expected.compact(), result.getResult().compact());
    }

    @Test
    public void testParseComponentWithSideBySideComponents() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff000))
                                .content("Text [lang]without."),
                        Component.text()
                                .content("formatting[/lang][lang]with.colors[/lang] more text"),
                        Component.text()
                                .color(TextColor.color(0x00ff00))
                                .content(" and this doesn't have placeholders")
                )
                .asComponent();

        TranslationResult result = parser.parseComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff000))
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
        assertEquals(expected.compact(), result.getResult().compact());
    }

    @Test
    public void testParseComponentWithOneArgument() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff000))
                                .content("Text [lang]without."),
                        Component.text("formatting.with.args[arg]test[/arg][/lang] more text")
                )
                .asComponent();

        TranslationResult result = parser.parseComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .content("Text ")
                                .color(TextColor.color(0xff000)),
                        Component.text()
                                .append(
                                        Component.text()
                                                .content("This is text without formatting but with arguments (")
                                                .color(TextColor.color(0xff000)),
                                        Component.text()
                                                .content("test")
                                                .color(TextColor.color(0x0000ff)),
                                        Component.text()
                                                .content(")")
                                                .color(TextColor.color(0xff000))
                                ),
                        Component.text()
                                .color(TextColor.color(0x0000ff))
                                .content(" more text")
                )
                .asComponent();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertEquals(expected.compact(), result.getResult().compact());
    }

    @Test
    public void testParseComponentWithTwoArguments() {
        Component comp = Component.text()
                .color(TextColor.color(0x0000ff))
                .append(
                        Component.text()
                                .color(TextColor.color(0xff000))
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

        TranslationResult result = parser.parseComponent(comp, configuration);

        Component expected = Component.text()
                .append(
                        Component.text()
                                .color(TextColor.color(0xff000))
                                .content("Text "),
                        Component.text()
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
        assertEquals(expected.compact(), result.getResult().compact());
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

        assertEquals(4, result.size());
        for (int i = 0; i < 4; i++) {
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

        assertEquals(7, result.size());
        for (int i = 0; i < 7; i++) {
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

        assertEquals(7, result.size());
        for (int i = 0; i < 7; i++) {
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

        assertEquals(5, result.size());
        for (int i = 0; i < 5; i++) {
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

        assertEquals(5, result.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    // TODO test splitting twice on same index before/after non-text component

}
