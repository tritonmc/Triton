package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.language.parser.LegacyParser.SerializedComponent;
import com.rexcantor64.triton.utils.DefaultFeatureSyntax;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LegacyParserTest {
    private final LegacyParser parser = new LegacyParser();
    private final FeatureSyntax defaultSyntax = new DefaultFeatureSyntax();

    private final Function<String, SerializedComponent> messageResolver = (key) -> {
        switch (key) {
            case "without.formatting":
                return new SerializedComponent("This is text without formatting");
            case "without.formatting.with.args":
                return new SerializedComponent("This is text without formatting but with arguments (%1)");
            case "with.colors":
                return new SerializedComponent("§aThis text is green");
            case "with.colors.two.args":
                return new SerializedComponent("§dThis text is pink and has two arguments (%1 and %2)");
            case "with.colors.repeated.args":
                return new SerializedComponent("§dThis text is pink and has three arguments (%1 and %2 and %1)");
            case "nested":
                return new SerializedComponent("some text [lang]without.formatting[/lang]");
            case "with.placeholder.colors":
                return new SerializedComponent("§d%1 §ais a very cool guy");
            case "change.colors.on.args":
                return new SerializedComponent("§cSome text §9%1 more text");
        }
        return new SerializedComponent("unknown placeholder");
    };

    private final TranslationConfiguration<SerializedComponent> configuration = new TranslationConfiguration<>(
            defaultSyntax,
            "disabled.line",
            (key, args) -> parser.replaceArguments(messageResolver.apply(key), args)
    );

    private final Component ALL_IN_ONE_COMPONENT = Component.text("Lorem ")
            .append(
                    Component.text("ipsum dolor ")
                            .color(NamedTextColor.BLACK)
                            .decorate(TextDecoration.BOLD)
                            .shadowColor(ShadowColor.shadowColor(0xdd, 0xee, 0xff, 0x44))
            )
            .append(
                    Component.text("sit amet,")
                            .decorate(TextDecoration.BOLD)
            )
            .append(
                    Component.text(" consectetur ")
                            .color(TextColor.color(0xaabbcc))
                            .clickEvent(ClickEvent.copyToClipboard("some text"))
                            .append(
                                    Component.text("adipiscing ")
                                            .font(Key.key("default"))
                            )
                            .append(
                                    Component.text("elit. ")
                                            .hoverEvent(HoverEvent.showText(Component.text("hello world")))
                            )
            )
            .append(
                    Component.text("Aenean tempor urna ac consequat sodales. ")
                            .shadowColor(ShadowColor.shadowColor(0xaa, 0xbb, 0xcc, 0x88))
            )
            .append(
                    Component.text("Maecenas imperdiet ")
                            .color(NamedTextColor.AQUA)
                            .append(
                                    Component.translatable(
                                            "some.key",
                                            Component.text("arg 1")
                                    )
                            )
            );

    @Test
    public void testSerializingComponent() {
        SerializedComponent serializedComponent = new SerializedComponent(ALL_IN_ONE_COMPONENT);

        assertEquals(1, serializedComponent.getClickEvents().size());
        assertEquals(1, serializedComponent.getHoverEvents().size());
        assertEquals(1, serializedComponent.getTranslatableComponents().size());
        assertEquals(
                "§rLorem §0§s#DDEEFF44§lipsum dolor §r§lsit amet,§x§a§a§b§b§c§c\uE4005"
                        + serializedComponent.getClickEvents().keySet().iterator().next()
                        + " consectetur §x§a§a§b§b§c§c\uE800minecraft:default\uE802adipiscing \uE801§x§a§a§b§b§c§c\uE500"
                        + serializedComponent.getHoverEvents().keySet().iterator().next()
                        + "elit. \uE501\uE401§r§s#AABBCC88Aenean tempor urna ac consequat sodales. §bMaecenas imperdiet §b\uE600some.key\uE600"
                        + serializedComponent.getTranslatableComponents().keySet().iterator().next()
                        + "\uE600",
                serializedComponent.getText()
        );
    }

    @Test
    public void testSerializeDeserializeComponent() {
        Component result = new SerializedComponent(ALL_IN_ONE_COMPONENT).toComponent();

        // slightly modify input to equivalent component, due to behaviour of the deserializer
        List<Component> expectedChildren = new ArrayList<>(ALL_IN_ONE_COMPONENT.children());
        expectedChildren.remove(expectedChildren.size() - 1); // remove last child
        Component expected = ALL_IN_ONE_COMPONENT.children(expectedChildren)
                .append(
                        Component.text("Maecenas imperdiet ")
                                .color(NamedTextColor.AQUA)
                )
                .append(
                        Component.translatable(
                                "some.key",
                                Component.text("arg 1")
                        ).color(NamedTextColor.AQUA)
                );

        assertEquals(expected.compact(), result.compact());
    }

    @Test
    public void testStripFormatting() {
        Component component = Component.text()
                .content("abc")
                .append(
                        Component.text()
                                .content("def")
                                .color(NamedTextColor.AQUA)
                )
                .append(Component.text("ghi"))
                .color(NamedTextColor.GOLD)
                .clickEvent(ClickEvent.copyToClipboard("Lorem Ipsum"))
                .hoverEvent(HoverEvent.showText(Component.text("Lorem Ipsum")))
                .build();

        SerializedComponent serializedComponent = new SerializedComponent(component);

        String result = parser.stripFormatting(serializedComponent.getText());

        assertEquals("abcdefghi", result);
    }

    @Test
    public void testTranslateComponentWithoutPlaceholders() {
        SerializedComponent comp = new SerializedComponent("Text without any placeholders whatsoever");

        TranslationResult<SerializedComponent> result = parser.translateComponent(comp, configuration);

        assertEquals(TranslationResult.ResultState.UNCHANGED, result.getState());
    }

    @Test
    public void testParseComponentWithoutFormatting() {
        SerializedComponent comp = new SerializedComponent("Text [lang]without.formatting[/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration)
                .map(SerializedComponent::toComponent);

        Component expected = Component.text("Text This is text without formatting more text");

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentStripFormattingFromKey() {
        SerializedComponent comp = new SerializedComponent("Text [lang]with§bout.formatting[/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration)
                .map(SerializedComponent::toComponent);

        Component expected = Component.text("Text This is text without formatting more text");

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentStyleSpillFromPlaceholder() {
        SerializedComponent comp = new SerializedComponent("Text [lang]with.colors[/lang] lorem ipsum [lang]without.formatting[/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration)
                .map(SerializedComponent::toComponent);

        Component expected = Component.text()
                .content("Text ")
                .append(
                        Component.text()
                                .content("This text is green lorem ipsum This is text without formatting more text")
                                .color(NamedTextColor.GREEN)
                )
                .build();

        assertEquals(TranslationResult.ResultState.CHANGED, result.getState());
        assertNotNull(result.getResultRaw());
        assertEquals(expected.compact(), result.getResultRaw().compact());
    }

    @Test
    public void testParseComponentStyleSpillFromArgument() {
        SerializedComponent comp = new SerializedComponent("Text §4[lang]without.formatting.with.args[args][arg]§agreen text[/arg][/args][/lang] more text");

        TranslationResult<Component> result = parser.translateComponent(comp, configuration)
                .map(SerializedComponent::toComponent);

        Component expected = Component.text()
                .content("Text ")
                .append(
                        Component.text()
                                .content("This is text without formatting but with arguments (")
                                .color(NamedTextColor.DARK_RED)
                )
                .append(
                        Component.text()
                                .content("green text) more text")
                                .color(NamedTextColor.GREEN)
                )
                .build();

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

        TranslationResult<Component> result = parser.translateComponent(new SerializedComponent(comp), configuration)
                .map(SerializedComponent::toComponent);

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

        TranslationResult<Component> result = parser.translateComponent(new SerializedComponent(comp), configuration)
                .map(SerializedComponent::toComponent);

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
        // TODO: make test pass
        Component comp = Component.text()
                .content("some text")
                .hoverEvent(HoverEvent.showText(Component.text("[lang]without.formatting[/lang]")))
                .asComponent();

        TranslationResult<Component> result = parser.translateComponent(new SerializedComponent(comp), configuration)
                .map(SerializedComponent::toComponent);

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
        // TODO: make test pass
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

        TranslationResult<Component> result = parser.translateComponent(new SerializedComponent(comp), configuration)
                .map(SerializedComponent::toComponent);

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
}
