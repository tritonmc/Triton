package com.rexcantor64.multilanguageplugin.language.item;

import java.util.HashMap;

public class LanguageText extends LanguageItem {

    private String key;
    private HashMap<String, String> languages;

    public LanguageText(String key, HashMap<String, String> languages) {
        super.type = LanguageItemType.TEXT;
        this.key = key;
        this.languages = languages;
    }

    public String getKey() {
        return key;
    }

    public String getMessage(String languageName) {
        return languages.get(languageName);
    }
}
