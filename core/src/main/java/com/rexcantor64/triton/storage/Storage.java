package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.player.LanguagePlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Storage {

    protected ConcurrentHashMap<String, Collection> collections = new ConcurrentHashMap<>();

    public abstract Language getLanguageFromIp(String ip);

    public abstract Language getLanguage(LanguagePlayer lp);

    public abstract void setLanguage(UUID uuid, String ip, Language newLanguage);

    public abstract void load();

    public boolean uploadToStorage() {
        throw new UnsupportedOperationException("Uploading to storage is not supported on local storage");
    }

}
