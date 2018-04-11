package com.rexcantor64.multilanguageplugin.language;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.components.api.ChatColor;
import com.rexcantor64.multilanguageplugin.config.interfaces.Configuration;
import com.rexcantor64.multilanguageplugin.language.item.LanguageItem;
import com.rexcantor64.multilanguageplugin.language.item.LanguageSign;
import com.rexcantor64.multilanguageplugin.language.item.LanguageText;
import com.rexcantor64.multilanguageplugin.player.LanguagePlayer;
import com.rexcantor64.multilanguageplugin.utils.YAMLUtils;

import java.util.ArrayList;
import java.util.List;

public class LanguageManager {

    private List<Language> languages = new ArrayList<>();
    private Language mainLanguage;
    private Multimap<LanguageItem.LanguageItemType, LanguageItem> items = ArrayListMultimap.create();

    public String getText(LanguagePlayer p, String code, Object... args) {
        return getText(p.getLang().getName(), code, args);
    }

    private String getText(String language, String code, Object... args) {
        for (LanguageItem item : items.get(LanguageItem.LanguageItemType.TEXT)) {
            LanguageText text = (LanguageText) item;
            if (!text.getKey().equals(code)) continue;
            String msg = text.getMessage(language);
            if (msg == null) return getTextFromMain(code, args);
            for (int i = 0; i < args.length; i++)
                msg = msg.replace("%" + Integer.toString(i + 1), args[i].toString());
            return ChatColor.translateAlternateColorCodes('&', msg);
        }
        return MultiLanguagePlugin.get().getMessage("error.message-not-found", "ERROR 404: Message not found: '%1'! Please notify the staff!", code);
    }

    private String getTextFromMain(String code, Object... args) {
        for (LanguageItem item : items.get(LanguageItem.LanguageItemType.TEXT)) {
            LanguageText text = (LanguageText) item;
            if (!text.getKey().equals(code)) continue;
            String msg = text.getMessage(mainLanguage.getName());
            if (msg == null) break;
            for (int i = 0; i < args.length; i++)
                msg = msg.replace("%" + Integer.toString(i + 1), args[i].toString());
            return ChatColor.translateAlternateColorCodes('&', msg);
        }
        return MultiLanguagePlugin.get().getMessage("error.message-not-found", "ERROR 404: Message not found: '%1'! Please notify the staff!", code);
    }

    public String[] getSign(LanguagePlayer p, LanguageSign.SignLocation location) {
        return getSign(p.getLang().getName(), location);
    }

    public String[] getSign(String language, LanguageSign.SignLocation location) {
        if (location == null) return null;
        for (LanguageItem item : items.get(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            if (!location.equals(sign.getLocation())) continue;
            String[] lines = sign.getLines(language);
            if (lines == null) return getSignFromMain(location);
            return lines;
        }
        return null;
    }

    private String[] getSignFromMain(LanguageSign.SignLocation location) {
        if (location == null) return null;
        for (LanguageItem item : items.get(LanguageItem.LanguageItemType.SIGN)) {
            LanguageSign sign = (LanguageSign) item;
            if (!location.equals(sign.getLocation())) continue;
            String[] lines = sign.getLines(mainLanguage.getName());
            if (lines == null) break;
            return lines;
        }
        return null;
    }

    public Language getLanguageByName(String name, boolean fallback) {
        if (name != null)
            for (Language lang : languages)
                if (lang.getName().equals(name))
                    return lang;
        if (fallback) return mainLanguage;
        return null;
    }

    public List<Language> getAllLanguages() {
        return new ArrayList<>(languages);
    }

    public List<LanguageItem> getAllItems(LanguageItem.LanguageItemType type) {
        return new ArrayList<>(items.get(type));
    }

    public Language getMainLanguage() {
        return mainLanguage;
    }

    public void setup() {
        languages.clear();
        mainLanguage = null;
        items.clear();
        MultiLanguagePlugin.get().logDebug("Setting up language manager...");
        Configuration languages = MultiLanguagePlugin.get().getConf().getLanguages();
        for (String lang : languages.getKeys())
            this.languages.add(mainLanguage = new Language(lang, languages.getString(lang + ".flag", "pa"), YAMLUtils.getStringOrStringList(languages, lang + ".minecraft-code"), languages.getString(lang + ".display-name", "&4Unknown")));
        this.mainLanguage = getLanguageByName(MultiLanguagePlugin.get().getConf().getMainLanguage(), true);
        for (LanguageItem item : MultiLanguagePlugin.get().getLanguageConfig().getItems())
            items.put(item.getType(), item);
        MultiLanguagePlugin.get().logDebug("Successfully setup the language manager! %1 languages and %2 language items loaded!", this.languages.size(), this.items.size());
    }

}
