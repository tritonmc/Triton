package com.rexcantor64.triton.api;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

/**
 * The entry point of the API
 *
 * @since 1.0.0
 */
public final class TritonAPI {
    @Internal
    private static Triton instance;

    /**
     * Get the instance of the {@link Triton plugin}.
     *
     * @return The instance of the {@link Triton plugin}.
     * @since 1.0.0
     */
    public static @NotNull Triton getInstance() {
        if (instance == null) {
            throw new UnsupportedOperationException("Triton is not running (yet?)! If you're seeing this, some plugin is trying to use the Triton API before Triton has loaded.");
        }
        return instance;
    }

    @SuppressWarnings("unused")
    @Internal
    private static void register(@NotNull Triton instance) {
        TritonAPI.instance = instance;
    }

    @Internal
    private TritonAPI() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

}
