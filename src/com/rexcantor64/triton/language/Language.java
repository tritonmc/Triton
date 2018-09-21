package com.rexcantor64.triton.language;

import com.rexcantor64.triton.banners.Banner;
import com.rexcantor64.triton.components.api.ChatColor;

import java.util.List;

public class Language {

    private String name;
    private List<String> minecraftCode;
    private String rawDisplayName;
    private String displayName;
    private Banner banner;
    private String flagCode;

    public Language(String name, String flagCode, List<String> minecraftCode, String displayName) {
        this.name = name;
        this.rawDisplayName = displayName;
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
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
