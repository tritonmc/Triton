package com.rexcantor64.triton.language.item;

import org.json.JSONArray;

import java.util.HashMap;

public class LanguageText extends LanguageItem {

    private String key;
    private HashMap<String, String> languages;

    public LanguageText(String key, HashMap<String, String> languages, boolean universal, boolean blacklist, JSONArray servers) {
        this.key = key;
        this.languages = languages;
        super.universal = universal;
        super.blacklist = blacklist;
        super.setServers(servers);
    }

    public LanguageText(String key, HashMap<String, String> languages) {
        this.key = key;
        this.languages = languages;
        super.setServers(null);
    }

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.TEXT;
    }

    public String getKey() {
        return key;
    }

    public String getMessage(String languageName) {
        return languages.get(languageName);
    }

    public HashMap<String, String> getLanguages() {
        return languages;
    }
}
