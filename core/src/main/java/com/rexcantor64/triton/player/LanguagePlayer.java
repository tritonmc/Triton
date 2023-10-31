package com.rexcantor64.triton.player;

import com.rexcantor64.triton.api.language.Language;

public interface LanguagePlayer extends com.rexcantor64.triton.api.players.LanguagePlayer {

    boolean isWaitingForClientLocale();

    void waitForClientLocale();

    @Override
    default Language getLanguage() {
        return this.getLang();
    }
}
