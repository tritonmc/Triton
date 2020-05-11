package com.rexcantor64.triton.language.item;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Data
public class LanguageText extends LanguageItem {

    private static final Pattern PATTERN = Pattern.compile("%(\\d+)");
    private final HashMap<String, String> languagesRegex = new HashMap<>();
    private HashMap<String, String> languages;
    private Boolean blacklist = null;
    private List<String> servers = null;
    private List<String> patterns = new ArrayList<>();

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.TEXT;
    }

    public String getMessage(String languageName) {
        return languages.get(languageName);
    }

    public String getMessageRegex(String languageName) {
        return languagesRegex.get(languageName);
    }

    public void generateRegexStrings() {
        languagesRegex.clear();
        for (Map.Entry<String, String> entry : languages.entrySet())
            languagesRegex
                    .put(entry.getKey(), PATTERN.matcher(entry.getValue().replace("$", "\\$")).replaceAll("\\$$1"));
    }
}
