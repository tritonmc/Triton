package com.rexcantor64.triton.utils;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Class with utility functions for splitting {@link Component}.
 *
 * @since 4.0.0
 */
public class ComponentSplitter {

    /**
     * Given a list of Components, splits them by text index, preserving style and hierarchy.
     * Non-text components (e.g. TranslatableComponent, KeybindComponent, etc.) are assumed to have a size of 1.
     *
     * @param component The Component to split
     * @param indexes   The indexes to split at
     * @return A list of the split Component lists
     * @since 4.0.0
     */
    public static List<Component> splitComponent(Component component, Queue<Integer> indexes) {
        return splitComponent(Collections.singletonList(component), new SplitState(indexes));
    }

    /**
     * @see ComponentSplitter#splitComponent(Component, Queue)
     */
    private static List<Component> splitComponent(List<Component> comps, SplitState state) {
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
    private static List<Component> flushAccumulator(List<Component> accumulator, List<Component> splits) {
        if (accumulator.isEmpty()) {
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
    private static List<Component> handleChildren(Component parent, List<Component> children, List<Component> accumulator, List<Component> splits, SplitState state) {
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
     * @since 4.0.0
     */
    private static Component convertEmptyComponent(Component component) {
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
     * (i.e. a call to {@link ComponentSplitter#splitComponent(Component, Queue)}).
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
}
