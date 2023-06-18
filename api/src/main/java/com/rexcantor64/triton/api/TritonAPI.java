package com.rexcantor64.triton.api;

import org.jetbrains.annotations.NotNull;

/**
 * The entry point of the API
 *
 * @since 1.0.0
 */
public class TritonAPI {

    /**
     * Get the instance of the {@link Triton plugin}.
     *
     * @return The instance of the {@link Triton plugin}.
     * @since 1.0.0
     */
    public static @NotNull Triton getInstance() {
        // This class gets replaced with a proper implementation in Triton's build.
        throw new UnsupportedOperationException("Triton is not running! If you're seeing this, it is because some plugin shadowed the TritonAPI (when it should not have!).");
    }

}
