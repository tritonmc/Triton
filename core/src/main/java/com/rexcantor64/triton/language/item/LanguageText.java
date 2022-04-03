package com.rexcantor64.triton.language.item;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Data
@EqualsAndHashCode(callSuper = true)
public class LanguageText extends LanguageItem {

    private static final Pattern PATTERN = Pattern.compile("%(\\d+)");
    private final HashMap<String, String> languagesRegex = new HashMap<>();
    private HashMap<String, String> languages;
    private Boolean blacklist = null;
    private List<String> servers = null;
    private List<String> patterns;

    @Override
    public LanguageItemType getType() {
        return LanguageItemType.TEXT;
    }

    public String getMessage(String languageName) {
        return languages == null ? null : languages.get(languageName);
    }

    public String getMessageRegex(String languageName) {
        return languagesRegex.get(languageName);
    }

    public void generateRegexStrings() {
        languagesRegex.clear();
        if (languages != null) {
            for (Map.Entry<String, String> entry : languages.entrySet()) {
                if (entry.getValue() == null) continue;

                languagesRegex
                        .put(entry.getKey(), PATTERN.matcher(entry.getValue().replace("$", "\\$")).replaceAll("\\$$1"));
            }
        }
    }

    public boolean belongsToServer(Collection.CollectionMetadata metadata, String serverName) {
        val blacklist = this.blacklist == null ? metadata.isBlacklist() : this.blacklist;
        val servers = this.servers == null ? metadata.getServers() : this.servers;
        if (blacklist) return !servers.contains(serverName);
        return servers.contains(serverName);
    }

}
