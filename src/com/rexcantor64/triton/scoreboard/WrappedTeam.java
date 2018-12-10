package com.rexcantor64.triton.scoreboard;

import com.rexcantor64.triton.components.api.chat.BaseComponent;
import com.rexcantor64.triton.components.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WrappedTeam {

    private final String name;
    private String prefix = "";
    private BaseComponent[] prefixComp = new BaseComponent[0];
    private String suffix = "";
    private BaseComponent[] suffixComp = new BaseComponent[0];
    private List<String> entries = new ArrayList<>();

    WrappedTeam(String name) {
        this.name = name;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public BaseComponent[] getPrefixComp() {
        return prefixComp;
    }

    public void setPrefixComp(BaseComponent[] prefixComp) {
        this.prefixComp = prefixComp;
        this.prefix = TextComponent.toLegacyText(prefixComp);
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public BaseComponent[] getSuffixComp() {
        return suffixComp;
    }

    public void setSuffixComp(BaseComponent[] suffixComp) {
        this.suffixComp = suffixComp;
        this.suffix = TextComponent.toLegacyText(suffixComp);
    }

    public void addEntry(Collection<String> entries) {
        for (String entry : entries)
            if (!this.entries.contains(entry)) {
                this.entries.add(entry);
            }
    }

    public void removeEntry(Collection<String> entries) {
        for (String entry : entries)
            this.entries.remove(entry);
    }

    public boolean hasEntry(String entry) {
        return this.entries.contains(entry);
    }

    public String getName() {
        return name;
    }

}
