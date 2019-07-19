package com.rexcantor64.triton.player;

import java.util.UUID;

public interface LanguagePlayer extends com.rexcantor64.triton.api.players.LanguagePlayer {

    void setBossbar(UUID uuid, String lastBossBar);

    void removeBossbar(UUID uuid);

    void setLastTabHeader(String lastTabHeader);

    void setLastTabFooter(String lastTabFooter);

    boolean isWaitingForClientLocale();

}
