package com.rexcantor64.triton.language.parser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.function.Consumer;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TranslationResult {
    private final ResultState state;
    private final Component result;

    /**
     * Run the given action if the result is CHANGED.
     *
     * @param action A consumer which will be passed the translation result
     * @return Itself, to allow chaining
     */
    public TranslationResult ifChanged(Consumer<Component> action) {
        if (state == ResultState.CHANGED) {
            action.accept(result);
        }
        return this;
    }

    /**
     * Run the given action if the result is REMOVE.
     *
     * @param action A runnable which will be run if the component should be removed/hidden/not sent
     * @return Itself, to allow chaining
     */
    public TranslationResult ifToRemove(Runnable action) {
        if (state == ResultState.REMOVE) {
            action.run();
        }
        return this;
    }


    /**
     * The given component did not have any placeholders and therefore was not changed.
     *
     * @return A {@link TranslationResult} representing an unchanged translation
     */
    public static TranslationResult unchanged() {
        return new TranslationResult(ResultState.UNCHANGED, null);
    }

    /**
     * The given component had one or more placeholders and therefore was changed.
     *
     * @param result The result component, after having its placeholders replaced
     * @return A {@link TranslationResult} representing a changed translation
     */
    public static TranslationResult changed(Component result) {
        return new TranslationResult(ResultState.CHANGED, result);
    }

    /**
     * The given component has a disabled line placeholder and therefore must be removed/deleted/not sent.
     *
     * @return A {@link TranslationResult} representing a removed translation
     */
    public static TranslationResult remove() {
        return new TranslationResult(ResultState.REMOVE, null);
    }

    public enum ResultState {
        UNCHANGED, CHANGED, REMOVE
    }
}