package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.localized.StringLocale;
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

}
