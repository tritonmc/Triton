package com.rexcantor64.triton.player;

import com.rexcantor64.triton.api.language.Language;

import java.util.UUID;

public interface LanguagePlayer extends com.rexcantor64.triton.api.players.LanguagePlayer {

    boolean isWaitingForClientLocale();

    void waitForClientLocale();

    @Override
    default Language getLanguage() {
        return this.getLang();
    }

    /**
     * @return the UUID that is to be used for storage-related operations
     */
    default UUID getStorageUniqueId() {
        return this.getUUID();
    }
}
