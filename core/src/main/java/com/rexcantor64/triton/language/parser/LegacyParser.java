package com.rexcantor64.triton.language.parser;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.utils.ParserUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.val;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.CharacterAndFormat;
import net.kyori.adventure.text.serializer.legacy.Reset;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.rexcantor64.triton.language.TranslationManager.JSON_TYPE_TAG;
import static com.rexcantor64.triton.language.TranslationManager.MINIMESSAGE_TYPE_TAG;

/**
 * A message parser that has the same behaviour as the "AdvancedComponent" parser on Triton v3 and below.
 * For backwards compatibility purposes.
 *
 * @since 4.0.0
 */
public class LegacyParser extends MessageParser {
    private static final char CLICK_DELIM = '\uE400';
    private static final char CLICK_END_DELIM = CLICK_DELIM + 1;
    private static final char HOVER_DELIM = '\uE500';
    private static final char HOVER_END_DELIM = HOVER_DELIM + 1;
    private static final char TRANSLATABLE_DELIM = '\uE600';
    private static final char KEYBIND_DELIM = '\uE700';
    private static final char FONT_START_DELIM = '\uE800';
    private static final char FONT_MID_DELIM = FONT_START_DELIM + 2;
    private static final char FONT_END_DELIM = FONT_START_DELIM + 1;

    private static final char AMPERSAND_CHAR = '&';
    private static final char SECTION_CHAR = '§';
    private static final char HEX_PREFIX = '#';
    private static final char HEX_CODE = 'x';
    private static final char SHADOW_COLOR_CODE = 's';
    private static final String VALID_COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";
    private static final Pattern FORMATTING_STRIP_PATTERN = Pattern.compile(
            CLICK_DELIM + "\\d[\\w-]{36}|" + HOVER_DELIM + "[\\w-]{36}|" + CLICK_END_DELIM + "|" + HOVER_END_DELIM
                    + "|" + SECTION_CHAR + "[0-9A-Fa-fK-Ok-oRrXx]"
    );

    /**
     * @see MessageParser#translateString(String, Localized, FeatureSyntax)
     * @since 4.0.0
     */
    @Override
    public @NotNull TranslationResult<String> translateString(@NotNull String text, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        Triton.get().getDumpManager().dump(Component.text(text), language, syntax);

        return translateComponent(
                new SerializedComponent(text),
                language,
                syntax
        )
                .map(SerializedComponent::toComponent)
                .map(ComponentUtils::serializeToLegacy);
    }

    /**
     * @see MessageParser#translateComponent(Component, Localized, FeatureSyntax)
     * @since 4.0.0
     */
    @Override
    public @NotNull TranslationResult<Component> translateComponent(@NotNull Component component, @NotNull Localized language, @NotNull FeatureSyntax syntax) {
        Triton.get().getDumpManager().dump(component, language, syntax);

        return translateComponent(
                new SerializedComponent(component),
                language,
                syntax
        ).map(SerializedComponent::toComponent);
    }

    private @NotNull TranslationResult<SerializedComponent> translateComponent(
            @NotNull SerializedComponent component,
            @NotNull Localized language,
            @NotNull FeatureSyntax syntax
    ) {
        val configuration = new TranslationConfiguration<SerializedComponent>(
                syntax,
                Triton.get().getConfig().getDisabledLine(),
                (key, arguments) -> Triton.get().getTranslationManager().getTextString(language, key)
                        .map(text -> this.handleTranslationType(text, language))
                        .map(comp -> replaceArguments(comp, arguments))
                        .orElseGet(() -> {
                            val notFoundComponent = new SerializedComponent(Triton.get().getTranslationManager().getTranslationNotFoundComponent());
                            val argsConcatenation = Arrays.stream(arguments).map(SerializedComponent::getText).collect(Collectors.joining(", "));
                            val argsConcatenationComp = new SerializedComponent("[" + argsConcatenation + "]");
                            for (SerializedComponent argument : arguments) {
                                argsConcatenationComp.importFromComponent(argument);
                            }

                            return replaceArguments(notFoundComponent, new SerializedComponent(key), argsConcatenationComp);
                        })
        );

        return translateComponent(component, configuration);
    }

    @VisibleForTesting
    @NotNull
    TranslationResult<SerializedComponent> translateComponent(
            @NotNull SerializedComponent component,
            @NotNull TranslationConfiguration<SerializedComponent> configuration
    ) {
        String text = component.getText();
        val indexes = ParserUtils.getPatternIndexArray(text, configuration.getFeatureSyntax().getLang());

        if (indexes.isEmpty()) {
            return handleNonContentText(component, configuration);
        }

        val builder = new StringBuilder();
        // keep track of last index added to the string builder
        int lastCharacter = 0;

        for (val index : indexes) {
            builder.append(text, lastCharacter, index[0]);
            lastCharacter = index[1];

            val placeholder = text.substring(index[2], index[3]);

            val resultComponent = handlePlaceholder(placeholder, configuration);
            if (!resultComponent.isPresent()) {
                return TranslationResult.remove();
            }

            builder.append(resultComponent.get().getText());
            component.importFromComponent(resultComponent.get());
        }

        builder.append(text, lastCharacter, text.length());
        component.setText(builder.toString());

        component = handleNonContentText(component, configuration)
                .getResult()
                .orElse(component);

        return TranslationResult.changed(component);
    }

    /**
     * An auxiliary method to {@link LegacyParser#translateComponent(SerializedComponent, TranslationConfiguration)}
     * that handles translating the component inside the <code>[lang][/lang]</code> tags.
     * The <code>[args][/args]</code> tags are optional since Triton v4.0.0.
     * <p>
     * This method gets the translation for the key and replaces its arguments, if any.
     *
     * @param placeholder   The text inside the <code>[lang][/lang]</code> tags.
     * @param configuration The settings to apply to this translation.
     * @return The translation of this placeholder. Empty optional if the translation is "disabled line".
     * @since 4.0.0
     */
    private @NotNull Optional<SerializedComponent> handlePlaceholder(
            @NotNull String placeholder,
            @NotNull TranslationConfiguration<SerializedComponent> configuration
    ) {
        val indexes = ParserUtils.getPatternIndexArray(placeholder, configuration.getFeatureSyntax().getArg());

        SerializedComponent[] arguments = indexes.stream()
                .map(index -> placeholder.substring(index[2], index[3]))
                .map(SerializedComponent::new)
                .toArray(SerializedComponent[]::new);

        String key = placeholder;
        if (!indexes.isEmpty()) {
            key = key.substring(0, indexes.get(0)[0]);
        }
        key = ParserUtils.normalizeTranslationKey(key, configuration);
        key = stripFormatting(key);

        val result = configuration.translationSupplier.apply(key, arguments);

        TranslationResult<SerializedComponent> translationResult = translateComponent(result, configuration);
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
    private TranslationResult<SerializedComponent> handleNonContentText(
            SerializedComponent component,
            TranslationConfiguration<SerializedComponent> configuration
    ) {
        boolean changed = false;
        for (val entry : component.hoverEvents.entrySet()) {
            HoverEvent<?> hoverEvent = entry.getValue();
            if (hoverEvent != null) {
                if (hoverEvent.action() == HoverEvent.Action.SHOW_TEXT) {
                    HoverEvent<Component> textHoverEvent = (HoverEvent<Component>) hoverEvent;
                    Component value = textHoverEvent.value();
                    TranslationResult<Component> result = translateComponent(new SerializedComponent(value), configuration)
                            .map(SerializedComponent::toComponent);
                    if (result.isToRemove()) {
                        changed = true;
                        entry.setValue(null);
                    }
                    if (result.getResult().isPresent()) {
                        changed = true;
                        entry.setValue(textHoverEvent.value(result.getResult().get()));
                    }
                } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ENTITY) {
                    HoverEvent<HoverEvent.ShowEntity> entityHoverEvent = (HoverEvent<HoverEvent.ShowEntity>) hoverEvent;
                    HoverEvent.ShowEntity value = entityHoverEvent.value();
                    if (value.name() != null) {
                        TranslationResult<Component> result = translateComponent(new SerializedComponent(value.name()), configuration)
                                .map(SerializedComponent::toComponent);
                        if (result.isToRemove()) {
                            changed = true;
                            entry.setValue(null);
                        }
                        if (result.getResult().isPresent()) {
                            changed = true;
                            entry.setValue(entityHoverEvent.value(value.name(result.getResult().get())));
                        }
                    }
                } else if (hoverEvent.action() == HoverEvent.Action.SHOW_ITEM) {
                    // TODO maybe use this library https://github.com/Eisenwave/eisen-nbt
                }
            }
        }

        for (val entry : component.translatableComponents.entrySet()) {
            TranslatableComponent translatableComponent = entry.getValue();
            AtomicBoolean argumentsChanged = new AtomicBoolean(false);
            List<TranslationArgument> translatedArguments = new ArrayList<>(translatableComponent.arguments().size());
            for (TranslationArgument argument : translatableComponent.arguments()) {
                if (argument.value() instanceof Component) {
                    Component argumentComp = (Component) argument.value();
                    translateComponent(new SerializedComponent(argumentComp), configuration)
                            .map(SerializedComponent::toComponent)
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
                entry.setValue(translatableComponent.arguments(translatedArguments));
            }
        }

        if (!changed) {
            return TranslationResult.unchanged();
        }
        return TranslationResult.changed(component);
    }

    public @NotNull String replaceArguments(@NotNull String text, @Nullable String @NotNull [] arguments) {
        for (int i = arguments.length - 1; i >= 0; --i) {
            text = text.replace("%" + (i + 1), String.valueOf(arguments[i]));
        }
        return text;
    }

    @VisibleForTesting
    @NotNull
    SerializedComponent replaceArguments(@NotNull SerializedComponent comp, @NotNull SerializedComponent @NotNull ... arguments) {
        // Replace args in text
        String[] args = Arrays.stream(arguments).map(SerializedComponent::getText).toArray(String[]::new);
        comp.setText(replaceArguments(comp.getText(), args));

        // Merge non-text parts (click, hover, etc.)
        for (SerializedComponent argument : arguments) {
            comp.importFromComponent(argument);
        }
        return comp;
    }

    /**
     * Lossy attempt at stitching together the given components.
     * Only used by API access and not by the plugin itself.
     * <p>
     * See {@link MessageParser#replaceArguments(Component, List)}
     */
    @Override
    public @NotNull Component replaceArguments(@NotNull Component component, @NotNull List<@NotNull Component> arguments) {
        val comp = new SerializedComponent(component);
        val args = arguments.stream().map(SerializedComponent::new).toArray(SerializedComponent[]::new);
        val result = replaceArguments(comp, args);
        return result.toComponent();
    }

    private @NotNull SerializedComponent handleTranslationType(@NotNull String message, @NotNull Localized language) {
        // TODO make minimsg the default (?)
        if (message.startsWith(MINIMESSAGE_TYPE_TAG)) {
            MiniMessage miniMessage = Triton.get().getTranslationManager().getMiniMessageInstanceForLanguage(language.getLanguage());
            return new SerializedComponent(miniMessage.deserialize(message.substring(MINIMESSAGE_TYPE_TAG.length())));
        } else if (message.startsWith(JSON_TYPE_TAG)) {
            return new SerializedComponent(GsonComponentSerializer.gson().deserialize(message.substring(JSON_TYPE_TAG.length())));
        } else {
            return new SerializedComponent(translateAlternateColorCodes(message));
        }
    }

    /**
     * Convert color codes with ampersand (&) into section characters (§).
     * The character is only converted if followed by a valid color code.
     * Inspired by md5's ChatColor#translateAlternateColorCodes.
     *
     * @param text The text to convert the color code characters from.
     * @return The input text with the color codes replaced.
     */
    @Contract("null -> null; !null -> !null")
    private @Nullable String translateAlternateColorCodes(@Nullable String text) {
        if (text == null) {
            return null;
        }

        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == AMPERSAND_CHAR && VALID_COLOR_CODES.indexOf(chars[i + 1]) != -1) {
                chars[i] = SECTION_CHAR;
            }
        }
        return new String(chars);
    }

    /**
     * Remove color code and click/hover event formatting in the text of {@link SerializedComponent}.
     *
     * @param text The text to remove the formatting from.
     * @return The text without the associated formatting.
     */
    @Contract("null -> null; !null -> !null")
    @VisibleForTesting
    @Nullable
    String stripFormatting(@Nullable String text) {
        if (text == null) {
            return null;
        }

        return FORMATTING_STRIP_PATTERN.matcher(text).replaceAll("");
    }

    /**
     * Represents a {@link Component} but as a String and with additional storage for the
     * values of click/hover events and translatable components.
     */
    @VisibleForTesting
    static class SerializedComponent {
        private final ComponentFlattener FLATTENER = ComponentFlattener.builder()
                .mapper(KeybindComponent.class, component -> KEYBIND_DELIM + component.keybind() + KEYBIND_DELIM)
                .mapper(TextComponent.class, TextComponent::content)
                .mapper(TranslatableComponent.class, component -> {
                    val uuid = UUID.randomUUID();
                    this.translatableComponents.put(uuid, component);

                    // Key is only included for backwards-compatibility
                    return TRANSLATABLE_DELIM + component.key() + TRANSLATABLE_DELIM + uuid + TRANSLATABLE_DELIM;
                })
                // ignore unknown components
                .build();

        @Getter
        private final HashMap<UUID, TranslatableComponent> translatableComponents = new HashMap<>();
        @Getter
        private final HashMap<UUID, ClickEvent> clickEvents = new HashMap<>();
        @Getter
        private final HashMap<UUID, @Nullable HoverEvent<?>> hoverEvents = new HashMap<>();
        @Getter
        @Setter(AccessLevel.PRIVATE)
        private String text;

        public SerializedComponent(Component component) {
            val flattenerListener = new CursedFlattenerListener();
            FLATTENER.flatten(component, flattenerListener);
            this.text = flattenerListener.toString();
        }

        public SerializedComponent(String legacyText) {
            this.text = legacyText;
        }

        /**
         * Converts this object back to an Adventure {@link Component}.
         *
         * @return The corresponding {@link Component}.
         */
        public @NotNull Component toComponent() {
            val list = toComponentList(this.text);
            if (list.isEmpty()) {
                return Component.empty();
            } else if (list.size() == 1) {
                return list.get(0);
            } else {
                return Component.text().append(list).build();
            }
        }

        private @NotNull List<@NotNull Component> toComponentList(@NotNull String text) {
            val list = new ArrayList<Component>();
            StringBuilder builder = new StringBuilder();
            TextComponent.Builder componentBuilder = Component.text();
            Style currentStyle = Style.empty(); // keep track of style of current component
            for (int i = 0; i < text.length(); i++) {
                val c = text.charAt(i);
                if (c == SECTION_CHAR) {
                    i++;
                    if (i >= text.length()) {
                        // last character in string, ignore
                        builder.append(c);
                        continue;
                    }

                    val lowercaseChar = Character.toLowerCase(text.charAt(i));
                    TextFormat format;
                    if (lowercaseChar == HEX_CODE && i + 12 < text.length()) {
                        val color = text.substring(i + 1, i + 13);
                        format = TextColor.fromHexString(HEX_PREFIX + color.replace(String.valueOf(SECTION_CHAR), ""));
                        i += 12;
                    } else if (lowercaseChar == SHADOW_COLOR_CODE && i + 9 < text.length()) {
                        @Subst("#ffffffff") val color = text.substring(i + 1, i + 10);
                        val shadowColor = ShadowColor.fromHexString(color);
                        currentStyle = currentStyle.shadowColor(shadowColor);
                        componentBuilder.style(currentStyle);
                        i += 9;
                        continue;
                    } else {
                        format = CharacterAndFormat.defaults().stream()
                                .filter(characterAndFormat -> characterAndFormat.character() == lowercaseChar)
                                .findFirst()
                                .map(CharacterAndFormat::format)
                                .orElse(null);
                    }
                    if (format == null) {
                        // unknown code, append color code as-is
                        builder.append(c);
                        i--;
                        continue;
                    }

                    if (builder.length() != 0) {
                        // there's already some text in the builder, flush the component before changing styles
                        componentBuilder.content(builder.toString());
                        builder = new StringBuilder();
                        list.add(componentBuilder.build());
                    }
                    componentBuilder = Component.text();
                    if (format instanceof TextColor) {
                        currentStyle = Style.style((TextColor) format);
                    } else if (format instanceof TextDecoration) {
                        currentStyle = currentStyle.decorate((TextDecoration) format);
                    } else if (format instanceof Reset) {
                        currentStyle = Style.empty();
                    }
                    componentBuilder.style(currentStyle);
                } else if (c == CLICK_DELIM || c == HOVER_DELIM || c == FONT_START_DELIM) {
                    if (builder.length() != 0) {
                        // there's already some text in the builder, flush the component before changing styles
                        componentBuilder.content(builder.toString());
                        builder = new StringBuilder();
                        list.add(componentBuilder.build());
                    }
                    componentBuilder = Component.text();
                    componentBuilder.style(currentStyle);

                    // handle styles/events based on delimiter found
                    switch (c) {
                        case CLICK_DELIM:
                            // action code is still present for compatibility only, but not used
                            val clickUuid = UUID.fromString(text.substring(i + 2, i + 2 + 36));
                            val click = this.clickEvents.get(clickUuid);
                            componentBuilder.clickEvent(click);
                            i += 2 + 36;
                            break;
                        case HOVER_DELIM:
                            val hoverUuid = UUID.fromString(text.substring(i + 1, i + 1 + 36));
                            val hover = this.hoverEvents.get(hoverUuid);
                            componentBuilder.hoverEvent(hover);
                            i += 1 + 36;
                            break;
                        case FONT_START_DELIM:
                            i++;
                            val font = new StringBuilder();
                            while (text.charAt(i) != FONT_MID_DELIM) {
                                font.append(text.charAt(i));
                                i++;
                            }
                            i++;
                            @Subst("minecraft:default") val fontName = font.toString();
                            componentBuilder.font(Key.key(fontName));
                            break;
                    }

                    // get the content until the corresponding delimiter
                    int deep = 0;
                    StringBuilder content = new StringBuilder();
                    while (text.charAt(i) != c + 1 || deep != 0) {
                        char c1 = text.charAt(i);
                        if (c1 == c) deep++; // c == \uE400 || c == \uE500 || c == \uE800
                        if (c1 == c + 1) deep--; // c + 1 == \uE401 || c + 1 == \uE501 || c + 1 == \uE801
                        content.append(c1);
                        i++;
                    }
                    List<Component> extra = toComponentList(content.toString());
                    if (!extra.isEmpty()) {
                        componentBuilder.append(extra);
                    }
                    list.add(componentBuilder.build());
                    componentBuilder = Component.text();
                    componentBuilder.style(currentStyle);
                } else if (c == TRANSLATABLE_DELIM) {
                    i++;
                    while (text.charAt(i) != TRANSLATABLE_DELIM) {
                        // ignore key (still here for backwards compatibility)
                        i++;
                    }
                    i++;
                    val uuid = new StringBuilder();
                    while (text.charAt(i) != TRANSLATABLE_DELIM) {
                        uuid.append(text.charAt(i));
                        i++;
                    }
                    if (builder.length() != 0) {
                        // there's already some text in the builder, flush the component before adding the component
                        componentBuilder.content(builder.toString());
                        builder = new StringBuilder();
                        list.add(componentBuilder.build());
                        componentBuilder = Component.text();
                        componentBuilder.style(currentStyle);
                    }
                    val translatableComponent = this.translatableComponents.get(UUID.fromString(uuid.toString()));
                    if (translatableComponent != null) {
                        list.add(translatableComponent.style(currentStyle));
                    }
                } else if (c == KEYBIND_DELIM) {
                    i++;
                    val key = new StringBuilder();
                    while (text.charAt(i) != KEYBIND_DELIM) {
                        key.append(text.charAt(i));
                        i++;
                    }
                    if (builder.length() != 0) {
                        // there's already some text in the builder, flush the component before adding the component
                        componentBuilder.content(builder.toString());
                        builder = new StringBuilder();
                        list.add(componentBuilder.build());
                        componentBuilder = Component.text();
                        componentBuilder.style(currentStyle);
                    }
                    val keybindComponent = Component.keybind().keybind(key.toString()).style(currentStyle).build();
                    list.add(keybindComponent);
                } else {
                    // just a normal character
                    builder.append(c);
                }
            }
            if (builder.length() != 0) {
                // flush remaining text to a new component
                componentBuilder.content(builder.toString());
                list.add(componentBuilder.build());
            }
            return list;
        }

        /**
         * Import non-text parts (click, hover, translatable) from the given component
         * into this component.
         * Useful for merging components while preserving their non-text parts.
         *
         * @param other The component to import non-text parts from.
         */
        public void importFromComponent(SerializedComponent other) {
            this.clickEvents.putAll(other.getClickEvents());
            this.hoverEvents.putAll(other.getHoverEvents());
            this.translatableComponents.putAll(other.getTranslatableComponents());
        }

        /**
         * Uses reserved unicode characters to delimit components/styles that cannot be represented with
         * legacy color codes, such as click, hover, translatable components, fonts, keybinds, etc.
         * The used characters are:
         * <ul>
         *     <li>\uE400 and \E401 for click events</li>
         *     <li>\uE500 and \E501 for hover events</li>
         *     <li>\uE600 for translatable components</li>
         *     <li>\uE700 for keybind components</li>
         *     <li>\uE800, \uE801 and \uE802 for fonts</li>
         * </ul>
         */
        private class CursedFlattenerListener implements FlattenerListener {
            private final StringBuilder stringBuilder = new StringBuilder();
            private Style[] stack = new Style[8];
            private int topIndex = -1;

            @Override
            public void component(@NotNull String text) {
                stringBuilder.append(text);
            }

            @Override
            public void pushStyle(@NotNull Style style) {
                val i = ++this.topIndex;
                if (i >= this.stack.length) {
                    this.stack = Arrays.copyOf(this.stack, this.stack.length * 2);
                }
                if (i > 0) {
                    style = this.stack[i - 1].merge(style);
                }
                this.stack[i] = style;

                @Nullable val color = style.color();
                if (color == null) {
                    this.stringBuilder.append(formatToString(Reset.INSTANCE));
                } else {
                    this.stringBuilder.append(formatToString(color));
                }

                @Nullable val shadowColor = style.shadowColor();
                if (shadowColor != null) {
                    this.stringBuilder.append(SECTION_CHAR).append('s');
                    // format: #RRGGBBAA
                    this.stringBuilder.append(shadowColor.asHexString());
                }

                style.decorations().entrySet().stream()
                        .filter(entry -> entry.getValue() == TextDecoration.State.TRUE)
                        .forEach(entry -> this.stringBuilder.append(formatToString(entry.getKey())));

                @Nullable val clickEvent = style.clickEvent();
                if (clickEvent != null && (i == 0 || !clickEvent.equals(this.stack[i - 1].clickEvent()))) {
                    val uuid = UUID.randomUUID();
                    SerializedComponent.this.clickEvents.put(uuid, clickEvent);

                    this.stringBuilder
                            .append(CLICK_DELIM)
                            .append(clickEvent.action().ordinal()) // backwards compatibility only
                            .append(uuid);
                }

                @Nullable val hoverEvent = style.hoverEvent();
                if (hoverEvent != null && (i == 0 || !hoverEvent.equals(this.stack[i - 1].hoverEvent()))) {
                    val uuid = UUID.randomUUID();
                    SerializedComponent.this.hoverEvents.put(uuid, hoverEvent);

                    this.stringBuilder
                            .append(HOVER_DELIM)
                            .append(uuid);
                }

                @Nullable val font = style.font();
                if (font != null && (i == 0 || !font.equals(this.stack[i - 1].font()))) {
                    this.stringBuilder
                            .append(FONT_START_DELIM)
                            .append(font.asString())
                            .append(FONT_MID_DELIM);
                }
            }

            @Override
            public void popStyle(@NotNull Style style) {
                val i = this.topIndex--;

                @Nullable val clickEvent = style.clickEvent();
                if (clickEvent != null && (i == 0 || !clickEvent.equals(this.stack[i - 1].clickEvent()))) {
                    this.stringBuilder.append(CLICK_END_DELIM);
                }

                @Nullable val hoverEvent = style.hoverEvent();
                if (hoverEvent != null && (i == 0 || !hoverEvent.equals(this.stack[i - 1].hoverEvent()))) {
                    this.stringBuilder.append(HOVER_END_DELIM);
                }

                @Nullable val font = style.font();
                if (font != null && (i == 0 || !font.equals(this.stack[i - 1].font()))) {
                    this.stringBuilder.append(FONT_END_DELIM);
                }
            }

            public String toString() {
                return this.stringBuilder.toString();
            }

            private @NotNull String formatToString(@NotNull TextFormat format) {
                if (format instanceof TextColor && !(format instanceof NamedTextColor)) {
                    // this is a hex color
                    final TextColor color = (TextColor) format;
                    String hexCode = String.format("%06x", color.value());
                    final StringBuilder legacy = new StringBuilder("" + SECTION_CHAR + HEX_CODE);
                    for (int i = 0, length = hexCode.length(); i < length; i++) {
                        legacy.append(SECTION_CHAR).append(hexCode.charAt(i));
                    }
                    return legacy.toString();
                }
                return CharacterAndFormat.defaults().stream()
                        .filter(characterAndFormat -> characterAndFormat.format().equals(format))
                        .findFirst()
                        .map(CharacterAndFormat::character)
                        .map(c -> "" + SECTION_CHAR + c)
                        .orElse("");
            }
        }
    }
}
