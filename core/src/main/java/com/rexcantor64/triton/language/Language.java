package com.rexcantor64.triton.language;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.banners.Banner;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
public class Language implements com.rexcantor64.triton.api.language.Language {

    private String name;
    private List<String> minecraftCodes;
    private String rawDisplayName;
    private List<String> fallbackLanguages = Collections.emptyList();
    private transient String displayName;
    @ToString.Exclude
    private transient Banner banner;
    private String flagCode;
    private List<ExecutableCommand> cmds = new ArrayList<>();

    public Language(String name, String flagCode, List<String> minecraftCodes, String displayName, List<String> fallbackLanguages, List<String> cmds) {
        this.name = name;
        this.rawDisplayName = displayName;
        this.minecraftCodes = minecraftCodes;
        this.flagCode = flagCode;
        if (fallbackLanguages != null) {
            this.fallbackLanguages = Collections.unmodifiableList(fallbackLanguages);
        }
        if (cmds != null) {
            for (String cmd : cmds) {
                this.cmds.add(ExecutableCommand.parse(cmd));
            }
        }
        computeProperties();
    }

    public void computeProperties() {
        this.displayName = this.rawDisplayName;
        if (Triton.isSpigot()) {
            this.banner = new Banner(flagCode, this.displayName);
        }
        // If loading with Gson, this might be set to null
        if (fallbackLanguages == null) {
            fallbackLanguages = Collections.emptyList();
        }
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
        return Objects.hash(name, minecraftCodes, rawDisplayName, flagCode, cmds);
    }

    @Override
    public Language getLanguage() {
        return this;
    }
}
