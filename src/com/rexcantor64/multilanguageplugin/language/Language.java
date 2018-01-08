package com.rexcantor64.multilanguageplugin.language;

import com.rexcantor64.multilanguageplugin.banners.Banner;
import com.rexcantor64.multilanguageplugin.components.api.ChatColor;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;

public class Language {

    private String name;
    private List<String> minecraftCode;
    private String rawDisplayName;
    private String displayName;
    private Banner banner;
    private String flagCode;

    Language(String name, String flagCode, List<String> minecraftCode, String displayName) {
        this.name = name;
        this.rawDisplayName = displayName;
        this.displayName = ChatColor.translateAlternateColorCodes('&', StringEscapeUtils.unescapeJava(displayName));
        banner = new Banner(flagCode, this.displayName);
        this.minecraftCode = minecraftCode;
        this.flagCode = flagCode;
    }

    public String getName() {
        return name;
    }

    public List<String> getMinecraftCodes() {
        return minecraftCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Banner getBanner() {
        return banner;
    }

    public String getRawDisplayName() {
        return rawDisplayName;
    }

    public String getFlagCode() {
        return flagCode;
    }
}
