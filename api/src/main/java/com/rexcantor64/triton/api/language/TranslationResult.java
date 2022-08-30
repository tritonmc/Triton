package com.rexcantor64.triton.api.language;

import net.kyori.adventure.text.Component;

import java.util.Optional;

public interface TranslationResult {

    /**
     * @return The result state of this translation.
     */
    ResultState getState();

    /**
     * @return If the state is {@link ResultState#CHANGED CHANGED}, returns the new {@link Component}. Otherwise, an empty optional.
     */
    Optional<Component> getChanged();

    enum ResultState {
        /**
         * The input did not contain any placeholders and therefore should not be changed.
         */
        UNCHANGED,
        /**
         * The input had at least one placeholder and has been changed accordingly.
         */
        CHANGED,
        /**
         * The input had a "disabled line" placeholder, and it should be handled as such.
         */
        REMOVE
    }
}
