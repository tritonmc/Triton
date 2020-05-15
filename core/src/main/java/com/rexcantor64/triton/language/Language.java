package com.rexcantor64.triton.language;

import com.rexcantor64.triton.banners.Banner;
import lombok.Data;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class Language implements com.rexcantor64.triton.api.language.Language {

    private String name;
    private List<String> minecraftCodes;
    private String rawDisplayName;
    private transient String displayName;
    private transient Banner banner;
    private String flagCode;
    private List<ExecutableCommand> cmds = new ArrayList<>();

    public Language(String name, String flagCode, List<String> minecraftCodes, String displayName, List<String> cmds) {
        this.name = name;
        this.rawDisplayName = displayName;
        this.minecraftCodes = minecraftCodes;
        this.flagCode = flagCode;
        if (cmds != null)
            for (String cmd : cmds)
                this.cmds.add(ExecutableCommand.parse(cmd));
        computeProperties();
    }

    public void computeProperties() {
        this.displayName = ChatColor.translateAlternateColorCodes('&', this.rawDisplayName);
        this.banner = new Banner(flagCode, this.displayName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Language language = (Language) o;
        return Objects.equals(name, language.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, minecraftCodes, rawDisplayName, displayName, banner, flagCode, cmds);
    }
}
