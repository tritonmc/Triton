package com.rexcantor64.triton.api;

import com.rexcantor64.triton.api.config.TritonConfig;
import com.rexcantor64.triton.api.language.LanguageManager;
import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.api.language.TranslationManager;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.api.players.PlayerManager;

/**
 * Represents the plugin's main class.
 *
 * @since 1.0.0
 */
public interface Triton {

    /**
     * Get the {@link TritonConfig config}.
     * Alias of {@link Triton#getConfig()}
     *
     * @return The {@link TritonConfig config}.
     * @since 1.0.0
     * @deprecated Use {@link Triton#getConfig()} instead.
     */
    @Deprecated
    default TritonConfig getConf() {
        return getConfig();
    }

    /**
     * Get the {@link TritonConfig config}.
     *
     * @return The {@link TritonConfig config}.
     * @since 3.0.0
     */
    TritonConfig getConfig();

    /**
     * Get the {@link LanguageManager language manager}.
     *
     * @return The {@link LanguageManager language manager}.
     * @since 1.0.0
     */
    LanguageManager getLanguageManager();

    /**
     * Get the {@link LanguageParser language parser}.
     *
     * @return The {@link LanguageParser language parser}.
     * @since 3.0.0
     * @deprecated Since 4.0.0. Use {@link Triton#getMessageParser()} instead.
     */
    @Deprecated
    LanguageParser getLanguageParser();

    /**
     * Get the {@link TranslationManager translation manager}.
     *
     * @return The {@link TranslationManager translation manager}.
     * @since 4.0.0
     */
    TranslationManager getTranslationManager();

    /**
     * Get the {@link MessageParser message parser}.
     *
     * @return The {@link MessageParser message parser}.
     * @since 4.0.0
     */
    MessageParser getMessageParser();

    /**
     * Get the {@link PlayerManager player manager}.
     *
     * @return The {@link PlayerManager player manager}.
     * @since 1.0.0
     */
    PlayerManager getPlayerManager();

    /**
     * Open the language selection GUI on a specific {@link LanguagePlayer player}.
     *
     * @param player The {@link LanguagePlayer player} that will see the GUI.
     * @since 1.0.0
     */
    void openLanguagesSelectionGUI(LanguagePlayer player);

    /**
     * Reload the config, messages, translations and player data.
     * Should not be abused.
     *
     * @since 2.5.0
     */
    void reload();

}
