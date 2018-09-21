package com.rexcantor64.triton.scoreboard;

import java.util.*;

public class TObjective {

    private String name;
    private String displayName;
    private boolean hearts;
    private HashMap<String, Integer> scores = new HashMap<>();
    private List<String> translatedScores = new ArrayList<>();
    private int displayPosition = -1;

    public TObjective(String name, String displayName, boolean hearts) {
        this.name = name;
        this.displayName = displayName;
        this.hearts = hearts;
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

    public boolean isHearts() {
        return hearts;
    }

    public void setHearts(boolean hearts) {
        this.hearts = hearts;
    }

    public void setScore(String entry, Integer score) {
        this.scores.put(entry, score);
    }

    public Integer getScore(String entry) {
        return this.scores.get(entry);
    }

    public void removeScore(String entry) {
        this.scores.remove(entry);
    }

    public Set<Map.Entry<String, Integer>> getScores() {
        return scores.entrySet();
    }

    public int getDisplayPosition() {
        return displayPosition;
    }

    public void setDisplayPosition(int displayPosition) {
        this.displayPosition = displayPosition;
    }

    public void addTranslatedScore(String entry) {
        translatedScores.add(entry);
    }

    public List<String> getTranslatedScores() {
        return translatedScores;
    }

    public void clearTranslatedScores() {
        translatedScores.clear();
    }
}
