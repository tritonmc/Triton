package com.rexcantor64.triton.api.language;

import com.rexcantor64.triton.api.players.LanguagePlayer;

import java.util.List;

/**
 * This class manages all the languages and messages.
 *
 * @since 1.0.0
 */
public interface LanguageManager {

    /**
     * Get a message from its code in a player's language.
     *
     * @param player The {@link LanguagePlayer LanguagePlayer} to get the language from. Use the {@link com.rexcantor64.triton.api.players.PlayerManager PlayerManager} to get it.
     * @param code   The code of the message to get.
     * @param args   (optional) The variables to replace in the message.
     * @return The message in the player's language. If no message is found with the provided code, a standard 404 message will be returned.
     * @since 1.0.0
     */
    String getText(LanguagePlayer player, String code, Object... args);

    /**
     * Get a message from its code in the main language.
     *
     * @param code The code of the message to get.
     * @param args (optional) The variables to replace in the message.
     * @return The message in the main language. If no message is found with the provided code, a standard 404 message will be returned.
     * @since 1.0.0
     */
    String getTextFromMain(String code, Object... args);

    /**
     * Get the 4 sign lines for a sign in a player's language.
     *
     * @param player   The {@link LanguagePlayer LanguagePlayer} to get the language from. Use the {@link com.rexcantor64.triton.api.players.PlayerManager PlayerManager} to get it.
     * @param location The location of the sign.
     * @return The message in the player's language. If no translatable sign is found on that location, null is returned.
     * @since 1.0.0
     */
    String[] getSign(LanguagePlayer player, SignLocation location);

    /**
     * Get a {@link Language language} by its name.
     *
     * @param name     The name of the {@link Language language}.
     * @param fallback Whether to return the main language or not if no language is found with the provided name.
     * @return The language with the provided name. If no language is found, the main language will be returned if fallback is true. Otherwise, null is returned.
     * @since 1.0.0
     */
    Language getLanguageByName(String name, boolean fallback);

    /**
     * Get a list of all the {@link Language languages}.
     *
     * @return A list of all the {@link Language languages}.
     * @since 1.0.0
     */
    List<Language> getAllLanguages();

    /**
     * Get the main (default) {@link Language language}.
     *
     * @return The main (default) {@link Language language}.
     * @since 1.0.0
     */
    Language getMainLanguage();
}
