package com.rexcantor64.triton.api.events;

import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * This event is triggered when a player changes their language.
 * To be used with BungeeCord.
 *
 * @since 2.4.0
 */
public class PlayerChangeLanguageBungeeEvent extends Event {
    private final LanguagePlayer languagePlayer;
    private final Language oldLanguage;
    private Language newLanguage;
    private boolean isCancelled;

    public PlayerChangeLanguageBungeeEvent(LanguagePlayer languagePlayer, Language oldLanguage, Language newLanguage) {
        this.languagePlayer = languagePlayer;
        this.oldLanguage = oldLanguage;
        this.newLanguage = newLanguage;
    }

    /**
     * Check if the event is cancelled.
     *
     * @return whether the event is cancelled or not.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Cancel the event.
     * If cancelled, the language of the player isn't changed.
     *
     * @param cancelled Set whether the event is cancelled or not.
     */
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     * Get the player that is changing languages.
     *
     * @return the player that is changing languages.
     */
    public LanguagePlayer getLanguagePlayer() {
        return languagePlayer;
    }

    /**
     * Get the language the player is switching from.
     *
     * @return the language the player is switching from.
     */
    public Language getOldLanguage() {
        return oldLanguage;
    }

    /**
     * Get the language the player is switching to.
     *
     * @return the language the player is switching to.
     */
    public Language getNewLanguage() {
        return newLanguage;
    }

    /**
     * Set the language the player is switching to.
     *
     * @param newLanguage the language the player is switching to.
     */
    public void setNewLanguage(Language newLanguage) {
        this.newLanguage = newLanguage;
    }

}
