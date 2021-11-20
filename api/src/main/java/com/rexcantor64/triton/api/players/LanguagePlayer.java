package com.rexcantor64.triton.api.players;

import com.rexcantor64.triton.api.language.Language;

import java.util.UUID;

/**
 * Represents a player that has a language.
 * You can get the instance of a player by using {@link PlayerManager#get(UUID)}.
 *
 * @since 1.0.0
 */
public interface LanguagePlayer {

    /**
     * Get the {@link Language language} of the player.
     *
     * @return The {@link Language language} of the player.
     * @since 1.0.0
     */
    Language getLang();

    /**
     * Set the player's {@link Language language}.
     *
     * @param language The {@link Language language} to set on the player.
     * @since 1.0.0
     */
    void setLang(Language language);

    /**
     * Refresh the player's translated messages. This includes scoreboard, tab, bossbars, entities, etc. Does not
     * affect chat.
     * This is automatically invoked when using {@link #setLang(Language)}.
     *
     * @since 1.0.0
     */
    void refreshAll();

    /**
     * Get the UUID of the player.
     *
     * @return The UUID of the player.
     * @since 2.4.0
     */
    UUID getUUID();
}
