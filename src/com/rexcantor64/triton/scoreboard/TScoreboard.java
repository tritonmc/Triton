package com.rexcantor64.triton.scoreboard;

import java.util.ArrayList;
import java.util.List;

public class TScoreboard {

    private List<TObjective> objectives = new ArrayList<>();
    private List<TTeam> teams = new ArrayList<>();

    public void addObjective(TObjective objective) {
        this.objectives.add(objective);
    }

    public void removeObjective(String objective) {
        this.objectives.remove(getObjective(objective));
    }

    public TObjective getObjective(String objective) {
        for (TObjective obj : this.objectives)
            if (obj.getName().equals(objective)) return obj;
        return null;
    }

    public List<TObjective> getAllObjectives() {
        return objectives;
    }

    public void addTeam(TTeam team) {
        this.teams.add(team);
    }

    public void removeTeam(String team) {
        this.teams.remove(getTeam(team));
    }

    public TTeam getTeam(String team) {
        for (TTeam obj : this.teams)
            if (obj.getName().equals(team)) return obj;
        return null;
    }

    public TTeam getEntryTeam(String entry) {
        for (TTeam team : teams)
            if (team.hasEntry(entry)) return team;
        return null;
    }

    public TObjective getVisibleObjective() {
        for (TObjective obj : this.objectives)
            if (obj.getDisplayPosition() == 1) return obj;
        return null;
    }

}
