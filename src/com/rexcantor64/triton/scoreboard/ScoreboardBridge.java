package com.rexcantor64.triton.scoreboard;

public interface ScoreboardBridge {

    boolean useComponents();

    void updateEntryScore(String entry, Integer score);

    void updateTeamPrefixSuffix(String prefix, String suffix, int index);

    void addEntryToTeam(String entry, int index);

    void initializeScoreboard(String title);

    void updateObjectiveTitle(String title);

}
