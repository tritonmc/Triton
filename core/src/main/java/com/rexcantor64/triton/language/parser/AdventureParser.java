package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.language.LanguageParser;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AdventureParser {

    private final static ComponentFlattener TEXT_ONLY_COMPONENT_FLATTENER = ComponentFlattener.builder()
            .mapper(TextComponent.class, TextComponent::content)
            .unknownMapper(comp -> "?")
            .build();
    private final static PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.builder()
            .flattener(TEXT_ONLY_COMPONENT_FLATTENER)
            .build();

    public TranslationResult parseComponent(Localized language, FeatureSyntax syntax, Component component) {
        TranslationConfiguration configuration = new TranslationConfiguration(
                syntax,
                // TODO properly integrate this
                (key) -> Component.text(Triton.get().getLanguageManager().getText(language, key))
        );
        return parseComponent(component, configuration);
    }

    @VisibleForTesting
    TranslationResult parseComponent(Component component, TranslationConfiguration configuration) {
        String plainText = componentToString(component);
        val indexes = LanguageParser.getPatternIndexArray(plainText, configuration.getFeatureSyntax().getLang());

        if (indexes.size() == 0) {
            return handleNonContentText(component, configuration);
        }

        Queue<Integer> indexesToSplitAt = indexes.stream()
                .flatMap(Arrays::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> splitComponents = splitComponent(component, indexesToSplitAt);
        List<Component> acc = new LinkedList<>();

        // Splits are cyclic: 0 is normal text, 1 is the placeholder start,
        // 2 is the inside of the placeholder and 3 is the placeholder end
        for (int i = 0; i < splitComponents.size(); i++) {
            if (i % 2 == 1) {
                // odd indexes are placeholder tags, ignore them
                continue;
            }
            Component part = splitComponents.get(i);
            if (i % 4 == 0) {
                // normal text, add to accumulator
                acc.add(part);
                continue;
            }
            acc.add(handlePlaceholder(part, configuration));
        }

        Component resultComponent = Component.join(JoinConfiguration.noSeparators(), acc);

        resultComponent = handleNonContentText(resultComponent, configuration)
                .getChanged()
                .orElse(resultComponent);

        return TranslationResult.changed(resultComponent);
    }

    private Component handlePlaceholder(Component placeholder, TranslationConfiguration configuration) {
        String placeholderStr = componentToString(placeholder);
        val indexes = LanguageParser.getPatternIndexArray(placeholderStr, configuration.getFeatureSyntax().getArg());
        Queue<Integer> indexesToSplitAt = indexes.stream()
                .flatMap(Arrays::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> splitComponents = splitComponent(placeholder, indexesToSplitAt);
        String key = "";
        List<Component> arguments = new LinkedList<>();

        // Splits are cyclic: 0 is normal text, 1 is the open tag,
        // 2 is the argument and 3 is the close tag
        for (int i = 0; i < splitComponents.size(); i++) {
            Component part = splitComponents.get(i);
            if (i == 0) {
                key = PlainTextComponentSerializer.plainText().serialize(part);
                // The [args] tag is optional since v4.0.0, so strip it if it's present
                if (key.endsWith("[" + configuration.getFeatureSyntax().getArgs() + "]")) {
                    key = key.substring(0, key.length() - configuration.getFeatureSyntax().getArgs().length() - 2);
                }
            }
            if (i % 4 == 2) {
                // Parse argument to allow nested placeholders
                parseComponent(part, configuration)
                        .ifChanged(arguments::add)
                        .ifToRemove(() -> arguments.add(Component.empty()))
                        .ifUnchanged(() -> arguments.add(part));
            }
        }

        Style defaultStyle = getStyleOfFirstCharacter(placeholder);
        Component result = configuration.translationSupplier.apply(key).applyFallbackStyle(defaultStyle);

        // TODO this should probably be parsed again, in case the resulting translation has placeholders
        return replaceArguments(result, arguments);
    }

    /**
     * Searches the given component's hover events and TranslatableComponent's arguments
     * for Triton placeholders, replacing them with the respective translations.
     * <p>
     * If a "disabled line" is present on a hover component, the hover component is discarded.
     * If a "disabled line" is present on a TranslatableComponent argument, that argument is set to empty.
     *
     * @param component     The component to search in.
     * @param configuration The translation configuration to use.
     * @return The given component with the placeholders replaced, wrapped in a TranslationResult.
     */
    @SuppressWarnings("unchecked")
    private TranslationResult handleNonContentText(Component component, TranslationConfiguration configuration) {
        boolean changed = false;
        HoverEvent<?> hoverEvent = component.hoverEvent();
        if (hoverEvent != null) {
            if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
                HoverEvent<Component> textHoverEvent = (HoverEvent<Component>) hoverEvent;
                Component value = textHoverEvent.value();
                TranslationResult result = parseComponent(value, configuration);
                if (result.getState() == TranslationResult.ResultState.REMOVE) {
                    changed = true;
                    component = component.hoverEvent(null);
                }
                if (result.getState() == TranslationResult.ResultState.CHANGED) {
                    changed = true;
                    component = component.hoverEvent(textHoverEvent.value(result.getResult()));
                }
            } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ENTITY) {
                // TODO
            } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
                // TODO
            }
        }

        if (component instanceof TranslatableComponent) {
            TranslatableComponent translatableComponent = (TranslatableComponent) component;
            AtomicBoolean argumentsChanged = new AtomicBoolean(false);
            List<Component> translatedArguments = new ArrayList<>(translatableComponent.args().size());
            for (Component argument : translatableComponent.args()) {
                parseComponent(argument, configuration)
                        .ifChanged(newArgument -> {
                            argumentsChanged.set(true);
                            translatedArguments.add(newArgument);
                        })
                        .ifUnchanged(() -> translatedArguments.add(argument))
                        .ifToRemove(() -> translatedArguments.add(Component.empty()));
            }

            if (argumentsChanged.get()) {
                changed = true;
                component = translatableComponent.args(translatedArguments);
            }
        }

        AtomicBoolean childrenChanged = new AtomicBoolean(false);
        List<Component> translatedChildren = new ArrayList<>(component.children().size());
        for (Component argument : component.children()) {
            parseComponent(argument, configuration)
                    .ifChanged(newArgument -> {
                        childrenChanged.set(true);
                        translatedChildren.add(newArgument);
                    })
                    .ifUnchanged(() -> translatedChildren.add(argument));
        }

        if (childrenChanged.get()) {
            changed = true;
            component = component.children(translatedChildren);
        }

        if (!changed) {
            return TranslationResult.unchanged();
        }
        return TranslationResult.changed(component);
    }

    /**
     * Recursively gets the styles applied to the first character in a component,
     * merging styles from parent components with child components.
     * If the component does not include text, an empty style is returned.
     *
     * @param component The component containing the text
     * @return The styles applied to the first character in the component
     */
    private Style getStyleOfFirstCharacter(Component component) {
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            if (!textComponent.content().isEmpty()) {
                return component.style();
            }
        }

        Iterator<Component> it = component.children().iterator();
        Style style = Style.empty();
        while (it.hasNext() && style.isEmpty()) {
            style = getStyleOfFirstCharacter(it.next());
        }

        style = component.style().merge(style);
        return style;
    }

    private String componentToString(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }

    private Component replaceArguments(Component component, List<Component> arguments) {
        PriorityQueue<PriorityPair<Component>> replacementMap = new PriorityQueue<>(Comparator.comparing(PriorityPair::getPriority));
        Queue<Integer> indexesToSplitAt = new LinkedList<>();
        String plainText = componentToString(component);

        boolean trackingNumber = false;
        int startIndex = 0;
        int number = 0;
        for (int i = 0; i < plainText.length(); ++i) {
            char c = plainText.charAt(i);
            if (trackingNumber) {
                // Check if we reached a non-digit
                if (c < '0' || c > '9') {
                    // flush
                    if (number > 0 && number <= arguments.size()) {
                        replacementMap.add(PriorityPair.of(arguments.get(number - 1), startIndex));
                        indexesToSplitAt.add(startIndex);
                        indexesToSplitAt.add(i);
                    }
                    trackingNumber = false;
                    startIndex = 0;
                    number = 0;
                } else {
                    int digit = c - '0';
                    number = number * 10 + digit;
                }
            }
            if (c == '%') {
                trackingNumber = true;
                startIndex = i;
            }
        }
        if (trackingNumber) {
            // flush
            if (number > 0 && number <= arguments.size()) {
                replacementMap.add(PriorityPair.of(arguments.get(number - 1), startIndex));
                indexesToSplitAt.add(startIndex);
            }
        }

        if (indexesToSplitAt.isEmpty()) {
            return component;
        }

        List<Component> splitComponents = splitComponent(component, indexesToSplitAt);
        List<Component> acc = new LinkedList<>();

        // Even indexes hold text, odd indexes should be discarded and replaced with arguments
        for (int i = 0; i < splitComponents.size(); ++i) {
            Component part = splitComponents.get(i);
            if (i % 2 == 0) {
                acc.add(part);
                continue;
            }
            acc.add(Objects.requireNonNull(replacementMap.poll()).getKey());
        }

        return Component.join(JoinConfiguration.noSeparators(), acc);
    }

    /**
     * Given a list of Components, splits them by text index, preserving style and hierarchy.
     * Non-text components (e.g. TranslatableComponent, KeybindComponent, etc.) are assumed to have a size of 1.
     *
     * @param component The Component to split
     * @param indexes   The indexes to split at
     * @return A list of the split Component lists
     */
    public List<Component> splitComponent(Component component, Queue<Integer> indexes) {
        return splitComponent(Collections.singletonList(component), new SplitState(indexes));
    }

    private List<Component> splitComponent(List<Component> comps, SplitState state) {
        List<Component> split = new LinkedList<>();
        List<Component> acc = new LinkedList<>();
        for (Component comp : comps) {
            if (!(comp instanceof TextComponent)) {
                while (state.checkAndConsumeSplitOfNonTextComponent()) {
                    acc.add(Component.empty());
                    acc = flushAccumulator(acc, split);
                }
                state.advanceBy(1); // non-text components always have length 1
                int beforeIndex = state.atIndex();
                acc = handleChildren(comp, comp.children(), acc, split, state);

                while (beforeIndex == state.atIndex() && state.checkAndConsumeSplitOfNonTextComponent()) {
                    acc = flushAccumulator(acc, split);
                    acc.add(Component.empty());
                }

                continue;
            }
            TextComponent textComponent = (TextComponent) comp;
            String[] textSplit = state.splitString(textComponent.content());
            for (int i = 0; i < textSplit.length; ++i) {
                Component newSplit = convertEmptyComponent(Component.text().content(textSplit[i]).mergeStyle(textComponent).build());
                if (i == textSplit.length - 1) {
                    // the last split keeps the extras
                    acc = handleChildren(newSplit, textComponent.children(), acc, split, state);
                } else {
                    acc.add(newSplit);
                    acc = flushAccumulator(acc, split);
                }
            }
        }

        flushAccumulator(acc, split);
        return split;
    }

    /**
     * Utility function to flush a Component accumulator.
     *
     * @param accumulator The accumulator to flush
     * @param splits      The result list to flush to
     * @return An empty LinkedList, as a new accumulator
     */
    private List<Component> flushAccumulator(List<Component> accumulator, List<Component> splits) {
        if (accumulator.size() == 0) {
            return accumulator;
        }

        if (accumulator.size() == 1) {
            splits.add(accumulator.get(0));
        } else {
            // wrap component list with empty component
            splits.add(Component.textOfChildren(accumulator.toArray(new Component[0])));
        }
        return new LinkedList<>();
    }

    /**
     * Utility function to handle splitting the children of a parent component.
     * Since components are immutable, this also adds the parent component to the accumulator.
     *
     * @param parent      The target component to place the children on
     * @param children    The children of the original component
     * @param accumulator The accumulator of the split process
     * @param splits      The split list of the split process
     * @param state       The state of the split process
     * @return The new accumulator
     */
    private List<Component> handleChildren(Component parent, List<Component> children, List<Component> accumulator, List<Component> splits, SplitState state) {
        if (children.isEmpty()) {
            accumulator.add(parent);
            return accumulator;
        }

        List<Component> extraSplit = splitComponent(children, state);
        for (int j = 0; j < extraSplit.size(); ++j) {
            if (j == 0) {
                // add the first split to the parent element
                parent = parent.children(Collections.singletonList(extraSplit.get(j)));
                accumulator.add(convertEmptyComponent(parent));
            } else {
                // flush accumulator before adding new sibling
                accumulator = flushAccumulator(accumulator, splits);
                Component extraWrapper = extraSplit.get(j);
                extraWrapper = extraWrapper.applyFallbackStyle(parent.style());
                accumulator.add(extraWrapper);
            }
        }
        return accumulator;
    }

    /**
     * Adventure has an issue where some components might not become empty
     * components, even though they should be. This is a fix for that, while
     * it doesn't get fixed upstream.
     * <a href="https://github.com/KyoriPowered/adventure/issues/807">Related GitHub Issue</a>
     *
     * @param component The component to check
     * @return The same component or an empty component
     */
    private Component convertEmptyComponent(Component component) {
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            if (textComponent.content().isEmpty() && textComponent.children().isEmpty() && textComponent.style().isEmpty()) {
                return Component.empty();
            }
        }
        return component;
    }

    @RequiredArgsConstructor
    private static class SplitState {
        final Queue<Integer> splitIndexes;
        int index;

        void advanceBy(int size) {
            this.index += size;
        }

        /**
         * @return true if there is a split at the beginning of this (non text) component
         */
        boolean checkAndConsumeSplitOfNonTextComponent() {
            if (!splitIndexes.isEmpty() && splitIndexes.peek() == atIndex()) {
                splitIndexes.remove();
                return true;
            }
            return false;
        }

        int atIndex() {
            return this.index;
        }

        String[] splitString(String str) {
            int lastIndex = 0;
            List<String> fragments = new ArrayList<>();

            while (!splitIndexes.isEmpty() && splitIndexes.peek() <= atIndex() + str.length()) {
                int i = splitIndexes.poll();
                fragments.add(str.substring(lastIndex, i - atIndex()));
                lastIndex = i - this.index;
            }
            fragments.add(str.substring(lastIndex));
            advanceBy(str.length());

            return fragments.toArray(new String[0]);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private static class PriorityPair<K> {
        final K key;
        final Integer priority;

        static <K> PriorityPair<K> of(K key, Integer priority) {
            return new PriorityPair<>(key, priority);
        }
    }

}
