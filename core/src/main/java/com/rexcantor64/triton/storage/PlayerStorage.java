package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.player.LanguagePlayer;

import java.util.UUID;

public interface PlayerStorage {

    Language getLanguageFromIp(String ip);

    Language getLanguage(LanguagePlayer lp);

    void setLanguage(UUID uuid, String ip, Language newLanguage);

}
