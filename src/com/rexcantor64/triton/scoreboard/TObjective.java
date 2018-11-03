package com.rexcantor64.triton.scoreboard;

import com.rexcantor64.triton.components.api.chat.BaseComponent;

import java.util.*;

public class TObjective {

    private String name;
    private String displayName;
    private BaseComponent[] displayChat;
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

    private static <K, V extends Comparable<? super V>>
    SortedSet<Map.Entry<K, V>> entriesSortedByValues(Map<K, V> map) {
        SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<>(
                (e1, e2) -> {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? -res : 1;
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    private static <E> E get(Collection<E> collection, int index) {
        Iterator<E> i = collection.iterator();
        E element = null;
        while (i.hasNext() && index-- >= 0) {
            element = i.next();
        }
        return element;
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

    public BaseComponent[] getDisplayChat() {
        return displayChat;
    }

    public void setDisplayChat(BaseComponent[] displayChat) {
        this.displayChat = displayChat;
    }

    public List<String> getTopScores() {
        SortedSet<Map.Entry<String, Integer>> set = entriesSortedByValues(scores);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 16 && i < set.size(); i++)
            result.add(get(set, i).getKey());
        return result;
    }
}
