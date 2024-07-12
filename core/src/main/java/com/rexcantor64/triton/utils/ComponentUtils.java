package com.rexcantor64.triton.utils;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.parser.AdventureParser;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ComponentUtils {

    public final static char SECTION_CHAR = '\u00A7';

    /**
     * Deserialize a JSON string representing a {@link Component}.
     *
     * @param json The JSON to deserialize.
     * @return The corresponding {@link Component}.
     */
    public static Component deserializeFromJson(@NotNull String json) {
        return GsonComponentSerializer.gson().deserialize(json);
    }

    /**
     * Serialize a {@link Component} to a JSON string.
     *
     * @param component The {@link Component} to serialize.
     * @return The corresponding JSON string.
     */
    public static String serializeToJson(@NotNull Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    /**
     * Given a {@link Component}, splits it by new lines, preserving style and hierarchy.
     *
     * @param component The {@link Component} to split by new lines (\n).
     * @return A list of the split {@link Component Components}.
     */
    public static List<Component> splitByNewLine(Component component) {
        val parser = Triton.get().getMessageParser();
        return splitByNewLine(component, parser);
    }

    @VisibleForTesting
    static List<Component> splitByNewLine(Component component, AdventureParser parser) {
        String plainText = parser.componentToString(component);
        Queue<Integer> indexesToSplitAt = IntStream.range(0, plainText.length())
                .filter(i -> plainText.charAt(i) == '\n')
                .flatMap(i -> IntStream.of(i, i + 1))
                .sorted()
                .boxed()
                .collect(Collectors.toCollection(LinkedList::new));

        if (indexesToSplitAt.size() == 0) {
            return Collections.singletonList(component);
        }

        List<Component> splitComponents = parser.splitComponent(component, indexesToSplitAt);

        // Splits are cyclic: 0 is normal text, 1 is the '\n' character
        return IntStream.range(0, splitComponents.size())
                .filter(i -> i % 2 == 0)
                .mapToObj(splitComponents::get)
                .collect(Collectors.toList());
    }

    /**
     * Given a {@link Component}, ensure it is not italic by setting italic to false if it's not set.
     * This is useful for translating item names and lores, where Minecraft makes them italic by default.
     * This does not do anything if the given component does not have any formatting whatsoever,
     * as to preserve the default Minecraft behaviour.
     *
     * @param component The components to check for italic
     * @return The component after it has been modified
     */
    public static Component ensureNotItalic(Component component) {
        if (!hasAnyFormatting(component)) {
            return component;
        }
        return component.applyFallbackStyle(Style.style().decoration(TextDecoration.ITALIC, false).build());
    }

    /**
     * Checks if the given component or any of its children have formatting.
     *
     * @param component The component to check for formatting
     * @return Whether there is any kind of formatting in the given component or its children
     */
    private static boolean hasAnyFormatting(Component component) {
        return !component.style().equals(Style.empty()) ||
                component.children().stream().anyMatch(ComponentUtils::hasAnyFormatting);
    }

    /**
     * Check whether the string has legacy styles (i.e. the SECTION_CHAR character).
     *
     * @param text The string to test.
     * @return Whether the text contains legacy styles.
     * @since 4.0.0
     */
    public static boolean hasLegacyFormatting(String text) {
        return text.indexOf(SECTION_CHAR) != -1;
    }

    /**
     * Replace all text components that contain legacy formatted text
     * (e.g. {"text": "§aHello world!"}) with their JSON counterpart.
     *
     * @param component The component to convert legacy formatter text from.
     * @return The new component, which will be the same as the input
     * if it did not contain legacy text.
     * @since 4.0.0
     */
    public static Component unflattenLegacyFormatting(final Component component) {
        boolean hasChanged = false;
        final List<Component> newChildren = new ArrayList<>(component.children().size());

        for (final Component child : component.children()) {
            final Component newChild = unflattenLegacyFormatting(child);
            if (child != newChild) {
                hasChanged = true;
            }
            newChildren.add(newChild);
        }

        // Only check the component itself last to avoid processing the generated children
        if (component instanceof TextComponent) {
            final TextComponent textComponent = (TextComponent) component;
            if (hasLegacyFormatting(textComponent.content())) {
                val deserializedComponent = LegacyComponentSerializer.legacySection()
                        .deserialize(textComponent.content());
                val children = new ArrayList<>(deserializedComponent.children());
                children.addAll(newChildren);
                return deserializedComponent.applyFallbackStyle(textComponent.style())
                        .children(children);
            }
        }

        if (hasChanged) {
            return component.children(newChildren);
        }
        return component;
    }

}
