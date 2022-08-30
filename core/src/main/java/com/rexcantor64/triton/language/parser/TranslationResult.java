package com.rexcantor64.triton.language.parser;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TranslationResult<T> implements com.rexcantor64.triton.api.language.TranslationResult<T> {
    @Getter
    private @NotNull
    final ResultState state;
    private final T result;

    @Override
    @Contract("_ -> this")
    public @NotNull TranslationResult<T> ifChanged(Consumer<T> action) {
        if (state == ResultState.CHANGED) {
            action.accept(result);
        }
        return this;
    }

    @Override
    @Contract("_ -> this")
    public @NotNull TranslationResult<T> ifToRemove(Runnable action) {
        if (state == ResultState.TO_REMOVE) {
            action.run();
        }
        return this;
    }

    @Override
    @Contract("_ -> this")
    public @NotNull TranslationResult<T> ifUnchanged(Runnable action) {
        if (state == ResultState.UNCHANGED) {
            action.run();
        }
        return this;
    }

    @Override
    public boolean isChanged() {
        return state == ResultState.CHANGED;
    }

    @Override
    public boolean isToRemove() {
        return state == ResultState.TO_REMOVE;
    }

    @Override
    public boolean isUnchanged() {
        return state == ResultState.UNCHANGED;
    }

    @Override
    public @NotNull Optional<T> getResult() {
        if (state == ResultState.CHANGED) {
            return Optional.of(this.result);
        }
        return Optional.empty();
    }

    @Override
    public @Nullable T getResultRaw() {
        return this.result;
    }

    @Override
    public @NotNull Optional<T> getResultOrToRemove(Supplier<@Nullable T> toRemoveSupplier) {
        switch (state) {
            case CHANGED:
                return Optional.of(this.result);
            case TO_REMOVE:
                return Optional.ofNullable(toRemoveSupplier.get());
            default:
                return Optional.empty();
        }
    }

    @Override
    @Contract("_ -> new")
    public <U> TranslationResult<U> map(Function<T, U> mappingFunction) {
        return new TranslationResult<>(this.getState(), this.getResult().map(mappingFunction).orElse(null));
    }

    @Override
    public <U> @Nullable U mapToObj(Function<@NotNull T, @Nullable U> changedSupplier, Supplier<@Nullable U> unchangedSupplier, Supplier<@Nullable U> toRemoveSupplier) {
        switch (this.state) {
            case CHANGED:
                return changedSupplier.apply(this.result);
            default:
            case UNCHANGED:
                return unchangedSupplier.get();
            case TO_REMOVE:
                return toRemoveSupplier.get();
        }
    }


    /**
     * The given input did not have any placeholders and therefore was not changed.
     *
     * @return A {@link TranslationResult} representing an unchanged translation
     */
    @Contract(" -> new")
    public static <T> @NotNull TranslationResult<T> unchanged() {
        return new TranslationResult<>(ResultState.UNCHANGED, null);
    }

    /**
     * The given input had one or more placeholders and therefore was changed.
     *
     * @param result The result value, after having its placeholders replaced
     * @return A {@link TranslationResult} representing a changed translation
     */
    @Contract("_ -> new")
    public static <T> @NotNull TranslationResult<T> changed(T result) {
        return new TranslationResult<>(ResultState.CHANGED, result);
    }

    /**
     * The given input has a disabled line placeholder and therefore must be removed/deleted/not sent.
     *
     * @return A {@link TranslationResult} representing a removed translation
     */
    @Contract(" -> new")
    public static <T> @NotNull TranslationResult<T> remove() {
        return new TranslationResult<>(ResultState.TO_REMOVE, null);
    }

}
