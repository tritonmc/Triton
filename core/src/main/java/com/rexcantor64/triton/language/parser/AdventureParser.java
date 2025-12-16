package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.utils.ComponentSplitter;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.utils.ParserUtils;
import com.rexcantor64.triton.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A message parser for Kyori's Adventure library.
 *
 * @since 4.0.0
 */
public class AdventureParser extends MessageParser {


    /**
     * @see MessageParser#translateString(String, Localized, FeatureSyntax)
     */
    @Override
    public @NotNull TranslationResult<String> translateString(@NotNull String text, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        return translateComponent(
                ComponentUtils.deserializeFromLegacy(text),
                language,
                syntax
        ).map(ComponentUtils::serializeToLegacy);
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
    public @NotNull TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        val configuration = new TranslationConfiguration<Component>(
                syntax,
                Triton.get().getConfig().getDisabledLine(),
                // TODO properly integrate this
                (key, arguments) -> Triton.get().getTranslationManager().getTextComponentOr404(language, key, arguments),
                Function.identity()
        );

        Triton.get().getDumpManager().dump(component, language, syntax);

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
    TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull TranslationConfiguration<Component> configuration) {
        String plainText = ComponentUtils.componentToString(component);

        if (ComponentUtils.hasLegacyFormatting(plainText)) {
            component = ComponentUtils.unflattenLegacyFormatting(component);
            plainText = ComponentUtils.componentToString(component);
        }

        val indexes = ParserUtils.getPatternIndexArray(plainText, configuration.getFeatureSyntax().getLang());

        if (indexes.isEmpty()) {
            return handleNonContentText(component, configuration);
        }

        Queue<Integer> indexesToSplitAt = indexes.stream()
                .flatMap(Arrays::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> splitComponents = ComponentSplitter.splitComponent(component, indexesToSplitAt);
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
    private Optional<Component> handlePlaceholder(Component placeholder, TranslationConfiguration<Component> configuration) {
        Style defaultStyle = getStyleOfFirstCharacterOrEmpty(placeholder);
        placeholder = stripStyleOfFirstCharacter(placeholder);

        String placeholderStr = ComponentUtils.componentToString(placeholder);
        val indexes = ParserUtils.getPatternIndexArray(placeholderStr, configuration.getFeatureSyntax().getArg());
        Queue<Integer> indexesToSplitAt = indexes.stream()
                .flatMap(Arrays::stream)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));

        List<Component> splitComponents = ComponentSplitter.splitComponent(placeholder, indexesToSplitAt);
        String key = "";
        List<Component> arguments = new LinkedList<>();

        // Splits are cyclic: 0 is normal text, 1 is the open tag,
        // 2 is the argument and 3 is the close tag
        for (int i = 0; i < splitComponents.size(); i++) {
            Component part = splitComponents.get(i);
            if (i == 0) {
                key = PlainTextComponentSerializer.plainText().serialize(part);
                key = ParserUtils.normalizeTranslationKey(key, configuration);
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
    private TranslationResult<Component> handleNonContentText(Component component, TranslationConfiguration<Component> configuration) {
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
            List<TranslationArgument> translatedArguments = new ArrayList<>(translatableComponent.arguments().size());
            for (TranslationArgument argument : translatableComponent.arguments()) {
                if (argument.value() instanceof Component) {
                    Component argumentComp = (Component) argument.value();
                    translateComponent(argumentComp, configuration)
                            .ifChanged(newArgument -> {
                                argumentsChanged.set(true);
                                translatedArguments.add(TranslationArgument.component(newArgument));
                            })
                            .ifUnchanged(() -> translatedArguments.add(argument))
                            .ifToRemove(() -> translatedArguments.add(TranslationArgument.component(Component.empty())));
                }
            }

            if (argumentsChanged.get()) {
                changed = true;
                component = translatableComponent.arguments(translatedArguments);
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
    @NotNull
    Component stripStyleOfFirstCharacter(@NotNull Component component) {
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
     * See {@link MessageParser#replaceArguments(Component, List)}
     */
    @Override
    public @NotNull Component replaceArguments(@NotNull Component component, @NotNull List<@NotNull Component> arguments) {
        PriorityQueue<PriorityPair<Component>> replacementMap = new PriorityQueue<>(Comparator.comparing(PriorityPair::getPriority));
        Queue<Integer> indexesToSplitAt = new LinkedList<>();
        String plainText = ComponentUtils.componentToString(component);

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

        List<Component> splitComponents = ComponentSplitter.splitComponent(component, indexesToSplitAt);
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
