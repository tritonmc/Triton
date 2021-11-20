package com.rexcantor64.triton.api.events;

import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is triggered when a player changes their language.
 * To be used with Spigot.
 *
 * @since 2.4.0
 */
public class PlayerChangeLanguageSpigotEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final LanguagePlayer languagePlayer;
    private final Language oldLanguage;
    private Language newLanguage;
    private boolean isCancelled;

    public PlayerChangeLanguageSpigotEvent(LanguagePlayer languagePlayer, Language oldLanguage, Language newLanguage) {
        this.languagePlayer = languagePlayer;
        this.oldLanguage = oldLanguage;
        this.newLanguage = newLanguage;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    /**
     * Check if the event is cancelled.
     *
     * @return whether the event is cancelled or not.
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Cancel the event.
     * If cancelled, the language of the player isn't changed.
     *
     * @param cancelled Set whether the event is cancelled or not.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
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
