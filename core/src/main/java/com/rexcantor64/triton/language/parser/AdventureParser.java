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
                acc.add(comp);
                continue;
            }
            TextComponent textComponent = (TextComponent) comp;
            String[] textSplit = state.splitString(textComponent.content());
            for (int i = 0; i < textSplit.length; ++i) {
                // TODO merge style
                TextComponent newSplit = Component.text().content(textSplit[i]).build();
                acc.add(newSplit);
                if (i == textSplit.length - 1) {
                    // the last split keeps the extras
                    if (!textComponent.children().isEmpty()) {
                        List<Component> extraSplit = splitComponent(textComponent.children(), state);
                        for (int j = 0; j < extraSplit.size(); ++j) {
                            if (j == 0) {
                                // the first split add to the parent element
                                newSplit = newSplit.children(Collections.singletonList(extraSplit.get(i)));
                            } else {
                                // flush accumulator before adding new sibling
                                if (acc.size() == 1) {
                                    split.add(acc.get(0));
                                } else {
                                    // wrap component list with empty component
                                    split.add(Component.textOfChildren(acc.toArray(new Component[0])));
                                }
                                acc = new LinkedList<>();
                                Component extraWrapper = extraSplit.get(j);
                                // TODO merge style
                                extraWrapper = extraWrapper.style(textComponent.style());
                                acc.add(extraWrapper);
                            }
                        }
                    }
                } else {
                    // flush accumulator
                    if (acc.size() == 1) {
                        split.add(acc.get(0));
                    } else {
                        // wrap component list with empty component
                        split.add(Component.textOfChildren(acc.toArray(new Component[0])));
                    }
                    acc = new LinkedList<>();
                }
            }
        }
        // flush accumulator
        if (acc.size() > 0) {
            if (acc.size() == 1) {
                split.add(acc.get(0));
            } else {
                // wrap component list with empty component
                split.add(Component.textOfChildren(acc.toArray(new Component[0])));
            }
        }
        return split;
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

        int atIndex() {
            return this.index;
        }

        String[] splitString(String str) {
            int lastIndex = 0;
            List<String> fragments = new ArrayList<>();

            while (!splitIndexes.isEmpty() && splitIndexes.peek() < atIndex() + str.length()) {
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
