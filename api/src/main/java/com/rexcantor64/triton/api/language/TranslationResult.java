package com.rexcantor64.triton.api.language;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface TranslationResult<T> {

    /**
     * @return The result state of this translation.
     */
    @NotNull ResultState getState();

    /**
     * @return If the state is {@link ResultState#CHANGED}, returns the new value. Otherwise, an empty optional.
     */
    @NotNull Optional<T> getResult();

    /**
     * @return Returns the result value without wrapping it into an Optional. Can be null.
     */
    @Nullable T getResultRaw();

    /**
     * Get the result or a default value if the state is {@link ResultState#TO_REMOVE}.
     *
     * @param toRemoveSupplier A supplier of a default value if the state is {@link ResultState#TO_REMOVE}.
     * @return The result value if the state is {@link ResultState#CHANGED} or the default value if the state is {@link ResultState#TO_REMOVE}.
     */
    @NotNull Optional<T> getResultOrToRemove(Supplier<@Nullable T> toRemoveSupplier);

    /**
     * Run the given action if the result is {@link ResultState#CHANGED}.
     *
     * @param action A consumer which will be passed the translation result
     * @return Itself, to allow chaining
     */
    @Contract("_ -> this")
    @NotNull TranslationResult<T> ifChanged(Consumer<T> action);

    /**
     * Run the given action if the result is {@link ResultState#TO_REMOVE}.
     *
     * @param action A runnable which will be run if the input should be removed/hidden/not sent
     * @return Itself, to allow chaining
     */
    @Contract("_ -> this")
    @NotNull TranslationResult<T> ifToRemove(Runnable action);

    /**
     * Run the given action if the result is {@link ResultState#UNCHANGED}.
     *
     * @param action A runnable which will be run if the input has not changed
     * @return Itself, to allow chaining
     */
    @Contract("_ -> this")
    @NotNull TranslationResult<T> ifUnchanged(Runnable action);

    /**
     * @return whether state is {@link ResultState#CHANGED}.
     */
    boolean isChanged();

    /**
     * @return whether state is {@link ResultState#TO_REMOVE}.
     */
    boolean isToRemove();

    /**
     * @return whether state is {@link ResultState#UNCHANGED}.
     */
    boolean isUnchanged();

    /**
     * Returns a new instance of this translation result, keeping the same state
     * and mapping the result value, if it exists, to a new value, according to the mapping function.
     *
     * @param mappingFunction The function to apply to the result value.
     * @param <U>             The result type of the new translation result.
     * @return A new {@link TranslationResult} with a result of type U.
     */
    @Contract("_ -> new")
    <U> TranslationResult<U> map(Function<T, U> mappingFunction);

    /**
     * Returns a single object, depending on the state of this {@link TranslationResult}.
     *
     * @param changedSupplier   The function to apply to the resulting value if state is {@link ResultState#CHANGED}.
     * @param unchangedSupplier The supplier to use if the state is {@link ResultState#UNCHANGED}.
     * @param toRemoveSupplier  The supplier to use if the state is {@link ResultState#TO_REMOVE}.
     * @param <U>               The type of the resulting object.
     * @return The value given by the respective supplier, depending on the state of this {@link TranslationResult}.
     */
    <U> @Nullable U mapToObj(Function<@NotNull T, @Nullable U> changedSupplier, Supplier<@Nullable U> unchangedSupplier, Supplier<@Nullable U> toRemoveSupplier);

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
        TO_REMOVE
    }
}
