package com.rexcantor64.triton.player;

import com.rexcantor64.triton.language.Language;

import java.util.UUID;

public interface LanguagePlayer {

    Language getLang();

    void setLang(Language language);

    void setBossbar(UUID uuid, String lastBossBar);

    void removeBossbar(UUID uuid);

    void setLastTabHeader(String lastTabHeader);

    void setLastTabFooter(String lastTabFooter);

    void refreshAll();

}
