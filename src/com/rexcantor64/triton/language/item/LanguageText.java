package com.rexcantor64.triton.language.item;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

public class LanguageText extends LanguageItem {

    private static final Pattern PATTERN = Pattern.compile("%(\\d+)");

    private HashMap<String, String> languages;
    private HashMap<String, String> languagesRegex = new HashMap<>();
    private boolean universal;
    private boolean blacklist;
    private List<String> servers = new ArrayList<>();
    private List<String> patterns;

    public LanguageText(String key, HashMap<String, String> languages, JSONArray patterns, boolean universal,
                        boolean blacklist, JSONArray servers) {
        this(key, languages, jsonArrayToStringList(patterns));
        this.universal = universal;
        this.blacklist = blacklist;
        this.servers = jsonArrayToStringList(servers);
    }

    public LanguageText(String key, HashMap<String, String> languages, List<String> patterns) {
        super(key);
        this.languages = languages;
        this.patterns = patterns;
    }

    private static List<String> jsonArrayToStringList(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array != null)
            for (Object obj : array)
                list.add(obj.toString());
        return list;
    }

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.TEXT;
    }

    public String getMessage(String languageName) {
        return languages.get(languageName);
    }

    public String getMessageRegex(String languageName) {
        if (languagesRegex.containsKey(languageName)) return languagesRegex.get(languageName);
        String input = languages.get(languageName);
        if (input == null) return null;
        input = PATTERN.matcher(input.replace("$", "\\$")).replaceAll("\\$$1");
        languagesRegex.put(languageName, input);
        return input;
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

    public List<String> getPatterns() {
        return patterns;
    }
}
