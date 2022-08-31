package com.rexcantor64.triton.api.players;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This class manages the instances of {@link LanguagePlayer LanguagePlayer}.
 *
 * @since 1.0.0
 */
public interface PlayerManager {

    /**
     * Get an instance of {@link LanguagePlayer LanguagePlayer} for the provided UUID.
     *
     * @param uuid The UUID of the player to get the {@link LanguagePlayer LanguagePlayer} instance from.
     * @return The instance of {@link LanguagePlayer LanguagePlayer} for the provided UUID.
     * @since 1.0.0
     */
    @NotNull LanguagePlayer get(@NotNull UUID uuid);

}
