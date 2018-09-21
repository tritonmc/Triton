package com.rexcantor64.triton.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TTeam {

    private String name;
    private String displayName;
    private String suffix;
    private String prefix;
    private String visibility;
    private String collision;
    private int color;
    private Object newColor;
    private List<String> entries = new ArrayList<>();
    private int optionData;

    public TTeam(String name, Collection<String> entries) {
        this.name = name;
        this.entries.addAll(entries);
    }

    public TTeam(String name, String displayName, String suffix, String prefix, String visibility, String collision, int color, List<String> entries, int optionData) {
        this.name = name;
        this.displayName = displayName;
        this.suffix = suffix;
        this.prefix = prefix;
        this.visibility = visibility;
        this.collision = collision;
        this.color = color;
        this.entries = entries;
        this.optionData = optionData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getCollision() {
        return collision;
    }

    public void setCollision(String collision) {
        this.collision = collision;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public void addEntry(String entry) {
        this.entries.add(entry);
    }

    public void removeEntry(String entry) {
        this.entries.remove(entry);
    }

    public boolean hasEntry(String entry) {
        return this.entries.contains(entry);
    }

    public List<String> getEntries() {
        return entries;
    }

    public int getOptionData() {
        return optionData;
    }

    public void setOptionData(int optionData) {
        this.optionData = optionData;
    }

    public Object getNewColor() {
        return newColor;
    }

    public void setNewColor(Object newColor) {
        this.newColor = newColor;
    }
}
