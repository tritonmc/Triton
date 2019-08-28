package com.rexcantor64.triton.language;

import com.rexcantor64.triton.banners.Banner;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Language implements com.rexcantor64.triton.api.language.Language {

    private String name;
    private List<String> minecraftCode;
    private String rawDisplayName;
    private String displayName;
    private Banner banner;
    private String flagCode;
    private List<ExecutableCommand> cmds = new ArrayList<>();

    public Language(String name, String flagCode, List<String> minecraftCode, String displayName, List<String> cmds) {
        this.name = name;
        this.rawDisplayName = displayName;
        this.displayName = ChatColor.translateAlternateColorCodes('&', displayName);
        banner = new Banner(flagCode, this.displayName);
        this.minecraftCode = minecraftCode;
        this.flagCode = flagCode;
        if (cmds != null)
            for (String cmd : cmds)
                this.cmds.add(ExecutableCommand.parse(cmd));
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

    public List<ExecutableCommand> getCmds() {
        return cmds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return Objects.equals(name, language.name);
    }

}
