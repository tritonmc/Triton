package com.rexcantor64.triton.language.item;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LanguageText extends LanguageItem {

    private HashMap<String, String> languages;
    private boolean universal;
    private boolean blacklist;
    private List<String> servers;

    public LanguageText(String key, HashMap<String, String> languages, boolean universal, boolean blacklist, JSONArray servers) {
        super(key);
        this.languages = languages;
        this.universal = universal;
        this.blacklist = blacklist;
        setServers(servers);
    }

    public LanguageText(String key, HashMap<String, String> languages) {
        super(key);
        this.languages = languages;
        setServers(null);
    }

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.TEXT;
    }

    public String getMessage(String languageName) {
        return languages.get(languageName);
    }

    public HashMap<String, String> getLanguages() {
        return languages;
    }

    public boolean isUniversal() {
        return universal;
    }

    public boolean isBlacklist() {
        return blacklist;
    }

    public List<String> getServers() {
        return servers;
    }

    private void setServers(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null)
            for (Object obj : array)
                list.add(obj.toString());
        this.servers = list;
    }
}
