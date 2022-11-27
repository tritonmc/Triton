package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.utils.StringUtils;
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
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A message parser for Kyori's Adventure library.
 *
 * @since 4.0.0
 */
public class AdventureParser implements MessageParser {

    private final static ComponentFlattener TEXT_ONLY_COMPONENT_FLATTENER = ComponentFlattener.builder()
            .mapper(TextComponent.class, TextComponent::content)
            .unknownMapper(comp -> "?")
            .build();
    private final static PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.builder()
            .flattener(TEXT_ONLY_COMPONENT_FLATTENER)
            .build();

    /**
     * @see MessageParser#translateString(String, Localized, FeatureSyntax)
     */
    @Override
    public @NotNull TranslationResult<String> translateString(String text, Localized language, FeatureSyntax syntax) {
        return translateComponent(
                LegacyComponentSerializer.legacySection().deserialize(text),
                language,
                syntax
        ).map(component -> LegacyComponentSerializer.legacySection().serialize(component));
    }

    /**
     * Find and replace Triton placeholders in a Component.
     * <p>
     * A translation can yield three states:
     * <ul>
     *     <li>placeholders are found and therefore translated;</li>
     *     <li>placeholders aren't found and therefore the component is left unchanged;</li>
     *     <li>a "disabled line" placeholder is found.</li>
     * </ul>
     * See {@link TranslationResult} for more details.
     *
     * @param component The component to find and replace Triton placeholders on.
     * @param language  The language to fetch translations on.
     * @param syntax    The syntax to use while searching for Triton placeholders.
     * @return The result of the translation
     * @since 4.0.0
     */
    @Override
    public @NotNull TranslationResult<Component> translateComponent(Component component, Localized language, FeatureSyntax syntax) {
        TranslationConfiguration configuration = new TranslationConfiguration(
                syntax,
                Triton.get().getConfig().getDisabledLine(),
                // TODO properly integrate this
                (key, arguments) -> Triton.get().getTranslationManager().getTextComponentOr404(language, key, arguments)
        );
        return translateComponent(component, configuration);
    }

    /**
     * @param component     The component to find and replace Triton placeholders on.
     * @param configuration The settings to apply to this translation.
     * @return The result of the translation
     * @see AdventureParser#translateComponent(Component, Localized, FeatureSyntax)
     * @since 4.0.0
     */
    @VisibleForTesting
    TranslationResult<Component> translateComponent(Component component, TranslationConfiguration configuration) {
        String plainText = componentToString(component);
        val indexes = this.getPatternIndexArray(plainText, configuration.getFeatureSyntax().getLang());

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
            Optional<Component> resultPlaceholder = handlePlaceholder(part, configuration);
            if (!resultPlaceholder.isPresent()) {
                return TranslationResult.remove();
            }
            acc.add(resultPlaceholder.get());
        }

        Component resultComponent = Component.join(JoinConfiguration.noSeparators(), acc);

        resultComponent = handleNonContentText(resultComponent, configuration)
                .getResult()
                .orElse(resultComponent);

        return TranslationResult.changed(resultComponent);
    }

    /**
     * An auxiliary method to {@link AdventureParser#translateComponent(Component, TranslationConfiguration)}
     * that handles translating the component inside the <code>[lang][/lang]</code> tags.
     * The <code>[args][/args]</code> tags are optional since Triton v4.0.0.
     * <p>
     * This method gets the translation for the key and replaces its arguments, if any.
     *
     * @param placeholder   The Component inside the <code>[lang][/lang]</code> tags.
     * @param configuration The settings to apply to this translation.
     * @return The translation of this placeholder. Empty optional if the translation is "disabled line".
     * @since 4.0.0
     */
    private Optional<Component> handlePlaceholder(Component placeholder, TranslationConfiguration configuration) {
        Style defaultStyle = getStyleOfFirstCharacterOrEmpty(placeholder);
        placeholder = stripStyleOfFirstCharacter(placeholder);

        String placeholderStr = componentToString(placeholder);
        val indexes = this.getPatternIndexArray(placeholderStr, configuration.getFeatureSyntax().getArg());
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
                if (!StringUtils.isEmptyOrNull(configuration.getDisabledLine()) && configuration.getDisabledLine()
                        .equals(key)) {
                    return Optional.empty();
                }
            }
            if (i % 4 == 2) {
                // Parse argument to allow nested placeholders
                translateComponent(part, configuration)
                        .ifChanged(arguments::add)
                        .ifToRemove(() -> arguments.add(Component.empty()))
                        .ifUnchanged(() -> arguments.add(part));
            }
        }

        Component result = configuration.translationSupplier.apply(key, arguments.toArray(new Component[0]))
                .applyFallbackStyle(defaultStyle);

        TranslationResult<Component> translationResult = translateComponent(result, configuration);
        if (translationResult.getState() == TranslationResult.ResultState.TO_REMOVE) {
            return Optional.empty();
        }

        return Optional.of(translationResult.getResult().orElse(result));
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
     * @since 4.0.0
     */
    @SuppressWarnings("unchecked")
    private TranslationResult<Component> handleNonContentText(Component component, TranslationConfiguration configuration) {
        boolean changed = false;
        HoverEvent<?> hoverEvent = component.hoverEvent();
        if (hoverEvent != null) {
            if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
                HoverEvent<Component> textHoverEvent = (HoverEvent<Component>) hoverEvent;
                Component value = textHoverEvent.value();
                TranslationResult<Component> result = translateComponent(value, configuration);
                if (result.isToRemove()) {
                    changed = true;
                    component = component.hoverEvent(null);
                }
                if (result.getResult().isPresent()) {
                    changed = true;
                    component = component.hoverEvent(textHoverEvent.value(result.getResult().get()));
                }
            } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ENTITY) {
                HoverEvent<HoverEvent.ShowEntity> entityHoverEvent = (HoverEvent<HoverEvent.ShowEntity>) hoverEvent;
                HoverEvent.ShowEntity value = entityHoverEvent.value();
                if (value.name() != null) {
                    TranslationResult<Component> result = translateComponent(value.name(), configuration);
                    if (result.isToRemove()) {
                        changed = true;
                        component = component.hoverEvent(null);
                    }
                    if (result.getResult().isPresent()) {
                        changed = true;
                        component = component.hoverEvent(entityHoverEvent.value(value.name(result.getResult().get())));
                    }
                }
            } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
                // TODO maybe use this library https://github.com/Eisenwave/eisen-nbt
            }
        }

        if (component instanceof TranslatableComponent) {
            TranslatableComponent translatableComponent = (TranslatableComponent) component;
            AtomicBoolean argumentsChanged = new AtomicBoolean(false);
            List<Component> translatedArguments = new ArrayList<>(translatableComponent.args().size());
            for (Component argument : translatableComponent.args()) {
                translateComponent(argument, configuration)
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
            handleNonContentText(argument, configuration)
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
     * If the component does not include text, an empty optional is returned.
     *
     * @param component The component containing the text
     * @return The styles applied to the first character in the component
     * @since 4.0.0
     */
    @VisibleForTesting
    Optional<Style> getStyleOfFirstCharacter(Component component) {
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            if (!textComponent.content().isEmpty()) {
                return Optional.of(component.style());
            }
        }

        Iterator<Component> it = component.children().iterator();
        Optional<Style> style = Optional.empty();
        while (it.hasNext() && !style.isPresent()) {
            style = getStyleOfFirstCharacter(it.next());
        }

        return style.map(s -> component.style().merge(s));
    }

    /**
     * Same as {@link AdventureParser#getStyleOfFirstCharacter(Component)} but
     * returns an empty style if the component does not include text.
     *
     * @see AdventureParser#getStyleOfFirstCharacter(Component)
     */
    private Style getStyleOfFirstCharacterOrEmpty(Component component) {
        return getStyleOfFirstCharacter(component).orElseGet(Style::empty);
    }

    /**
     * Recursively removes the styles applied to the first character in a component.
     * This is so styles from higher up in the tree are ignored while splitting arguments.
     *
     * @param component The component to remove the styles from
     * @return The new component without styles
     * @since 4.0.0
     */
    @VisibleForTesting
    @Contract("_ -> new")
    @NotNull Component stripStyleOfFirstCharacter(@NotNull Component component) {
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            if (!textComponent.content().isEmpty()) {
                return component.style(Style.empty());
            }
        }

        ArrayList<Component> newChildren = new ArrayList<>(component.children().size());
        Iterator<Component> it = component.children().iterator();
        boolean foundFirstCharacter = false;
        while (it.hasNext()) {
            Component next = it.next();
            if (foundFirstCharacter) {
                newChildren.add(next.applyFallbackStyle(component.style()));
                continue;
            }
            Component result = stripStyleOfFirstCharacter(next);
            newChildren.add(result);
            if (result != next) {
                foundFirstCharacter = true;
            }
        }

        if (!foundFirstCharacter) {
            return component;
        }

        newChildren.trimToSize();

        return component.style(Style.empty()).children(newChildren);
    }

    /**
     * Serializes a Component as a string, replacing
     * non-text components with a '?' (question mark) character.
     *
     * @param component The component to serialize.
     * @return The serialization result.
     * @since 4.0.0
     */
    public String componentToString(Component component) {
        return PLAIN_TEXT_SERIALIZER.serialize(component);
    }

    /**
     * Given a Component with "%1", "%2", etc., replace these with the given arguments.
     *
     * @param component The component with % placeholders.
     * @param arguments The list of arguments available to use as replacements.
     * @return The component with % placeholders replaced with arguments.
     * @since 4.0.0
     */
    public Component replaceArguments(Component component, List<Component> arguments) {
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
            acc.add(
                    Objects.requireNonNull(replacementMap.poll())
                            .getKey()
                            // apply the sames styles to the replacement as "%X" had
                            .applyFallbackStyle(getStyleOfFirstCharacterOrEmpty(part))
            );
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
     * @since 4.0.0
     */
    public List<Component> splitComponent(Component component, Queue<Integer> indexes) {
        return splitComponent(Collections.singletonList(component), new SplitState(indexes));
    }

    /**
     * @see AdventureParser#splitComponent(Component, Queue)
     */
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
                Component newSplit = convertEmptyComponent(Component.text()
                        .content(textSplit[i])
                        .mergeStyle(textComponent)
                        .build());
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
     * @since 4.0.0
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
     * @since 4.0.0
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
     * Find the indexes of all root "[pattern][/pattern]" tags in the given string.
     * <p>
     * Only the root tags are included, that is, nested tags are ignored.
     * For example, <code>[pattern][pattern][/pattern][/pattern]</code> would only
     * return the indexes for the outer tags.
     * <p>
     * Each array in the returned list corresponds to a different set of opening and closing tags,
     * and has size 4.
     * Indexes have the following meaning:
     * <ul>
     *     <li>0: the first character of the opening tag</li>
     *     <li>1: the character after the last character of the closing tag</li>
     *     <li>2: the character after the last character of the opening tag</li>
     *     <li>3: the first character of the closing tag</li>
     * </ul>
     *
     * @param input   The string to search for opening and closing tags.
     * @param pattern The tags to search for (i.e. "lang" will search for "[lang]" and "[/lang]").
     * @return A list of indexes of all the found tags, as specified by the method description.
     */
    public List<Integer[]> getPatternIndexArray(String input, String pattern) {
        List<Integer[]> result = new ArrayList<>();
        int start = -1;
        int openedAmount = 0;

        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (currentChar == '[' && input.length() > i + pattern.length() + 1 && input.substring(i + 1,
                    i + 2 + pattern.length()).equals(pattern + "]")) {
                if (start == -1) start = i;
                openedAmount++;
                i += 1 + pattern.length();
            } else if (currentChar == '[' && input.length() > i + pattern.length() + 2 && input.substring(i + 1,
                    i + 3 + pattern.length()).equals("/" + pattern + "]")) {
                openedAmount--;
                if (openedAmount == 0) {
                    result.add(new Integer[]{start, i + 3 + pattern.length(), start + pattern.length() + 2, i});
                    start = -1;
                }
            }
        }
        return result;
    }

    /**
     * Adventure has an issue where some components might not become empty
     * components, even though they should be. This is a fix for that, while
     * it doesn't get fixed upstream.
     * <a href="https://github.com/KyoriPowered/adventure/issues/807">Related GitHub Issue</a>
     *
     * @param component The component to check
     * @return The same component or an empty component
     * @since 4.0.0
     */
    private Component convertEmptyComponent(Component component) {
        if (component instanceof TextComponent) {
            TextComponent textComponent = (TextComponent) component;
            if (textComponent.content().isEmpty() && textComponent.children().isEmpty() && textComponent.style()
                    .isEmpty()) {
                return Component.empty();
            }
        }
        return component;
    }

    /**
     * Holds the state for a component split action
     * (i.e. a call to {@link AdventureParser#splitComponent(Component, Queue)}).
     *
     * @since 4.0.0
     */
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

    /**
     * A weighted value to be used in a priority queue.
     *
     * @param <K> The type of the value this object holds.
     * @since 4.0.0
     */
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
