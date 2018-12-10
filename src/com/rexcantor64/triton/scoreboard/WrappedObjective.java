package com.rexcantor64.triton.scoreboard;

import com.rexcantor64.triton.components.api.chat.BaseComponent;

import java.util.*;

public class WrappedObjective {

    private final String name;
    private String title = "";
    private BaseComponent[] titleComp = new BaseComponent[0];

    private HashMap<String, Integer> scores = new HashMap<>();
    private boolean scoresModified = true;

    public WrappedObjective(String name) {
        this.name = name;
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

    public boolean hasChanges() {
        return scoresModified;
    }

    public void resetModified() {
        scoresModified = false;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BaseComponent[] getTitleComp() {
        return titleComp;
    }

    public void setTitleComp(BaseComponent[] titleComp) {
        this.titleComp = titleComp;
    }

    public void setScore(String entry, Integer score) {
        if (score == null) {
            if (this.scores.remove(entry) != null) scoresModified = true;
            return;
        }
        if (!score.equals(this.scores.put(entry, score))) scoresModified = true;
    }

    public Integer getScore(String entry) {
        return scores.get(entry);
    }

    public List<String> getTopScores() {
        SortedSet<Map.Entry<String, Integer>> set = entriesSortedByValues(scores);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < 16 && i < set.size(); i++)
            result.add(get(set, i).getKey());
        return result;
    }

}
