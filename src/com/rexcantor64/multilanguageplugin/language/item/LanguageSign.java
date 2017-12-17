package com.rexcantor64.multilanguageplugin.language.item;

import org.bukkit.Location;

import java.util.HashMap;

public class LanguageSign extends LanguageItem {

    private Location location;
    private HashMap<String, String[]> languages;

    LanguageSign(Location location, HashMap<String, String[]> languages) {
        super.type = LanguageItemType.SIGN;
        this.location = location;
        this.languages = languages;
    }

    public Location getLocation() {
        return location;
    }

    public String[] getLines(String languageName) {
        return languages.get(languageName);
    }
}
