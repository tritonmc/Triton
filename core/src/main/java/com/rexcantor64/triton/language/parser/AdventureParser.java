package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.language.LanguageParser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class AdventureParser {

    public Component parseComponent(Localized language, FeatureSyntax syntax, Component component) {
        return parseComponent(component, new State(syntax, language));
    }

    private Component parseComponent(Component component, State state) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        val indexes = LanguageParser.getPatternIndexArray(plainText, state.getFeatureSyntax().getLang());

        Queue<Integer> indexesToSplitAt = indexes.stream()
                .flatMap(Arrays::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> splittedComponents = splitComponent(component, indexesToSplitAt);

        // TODO

        return component;
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
                if (state.advanceByAndCheckSplitOfNonTextComponent()) {
                    acc = flushAccumulator(acc, split);
                }
                acc = handleChildren(comp, comp.children(), acc, split, state);
                continue;
            }
            TextComponent textComponent = (TextComponent) comp;
            String[] textSplit = state.splitString(textComponent.content());
            for (int i = 0; i < textSplit.length; ++i) {
                TextComponent newSplit = Component.text().content(textSplit[i]).mergeStyle(textComponent).build();
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
                accumulator.add(parent);
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

    public enum Result {
        UNCHANGED, CHANGED, REMOVE
    }

    @Data
    private static class State {
        final FeatureSyntax featureSyntax;
        final Localized targetLocale;
        Result result;
    }

    @RequiredArgsConstructor
    private static class SplitState {
        final Queue<Integer> splitIndexes;
        int index;

        void advanceBy(int size) {
            this.index += size;
        }

        /**
         * @return true if there is a split at the beginning of this component
         */
        boolean advanceByAndCheckSplitOfNonTextComponent() {
            this.advanceBy(1);
            return !splitIndexes.isEmpty() && splitIndexes.peek() == atIndex() - 1;
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

}
