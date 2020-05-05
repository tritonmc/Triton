package com.rexcantor64.triton.scoreboard;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import com.rexcantor64.triton.utils.ScoreboardUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.ArrayList;
import java.util.List;

public class WrappedScoreboard {

    private final ScoreboardBridge bridge;
    private final SpigotLanguagePlayer owner;

    private ScoreboardLine[] clientRender = new ScoreboardLine[15];
    private List<String> topEntries = new ArrayList<>();

    private List<WrappedTeam> teams = new ArrayList<>();
    private List<WrappedObjective> objectives = new ArrayList<>();

    private WrappedObjective sidebarObjective = null;
    private boolean initialized = false;

    public WrappedScoreboard(ScoreboardBridge bridge, SpigotLanguagePlayer owner) {
        this.bridge = bridge;
        this.owner = owner;
        for (int i = 0; i < 15; i++)
            clientRender[i] = new ScoreboardLine();
    }

    public void rerender(boolean force) {
        if (sidebarObjective == null) return;
        if (force)
            bridge.updateObjectiveTitle(bridge.useComponents() ? ComponentSerializer
                    .toString(Triton.get().getLanguageParser()
                            .parseComponent(owner, Triton.get().getConf().getScoreboardSyntax(), sidebarObjective
                                    .getTitleComp())) : translate(owner, sidebarObjective.getTitle(), 32, Triton.get()
                    .getConf().getScoreboardSyntax()));

        if (sidebarObjective.hasChanges()) topEntries = sidebarObjective.getTopScores();
        sidebarObjective.resetModified();
        List<String> toRemove = new ArrayList<>();
        List<String> doNotRemove = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            ScoreboardLine line = clientRender[i];
            if (i >= topEntries.size()) {
                if (line.score != null) {
                    line.score = null;
                    bridge.updateEntryScore(line.entry + (bridge.useComponents() ? "" : ScoreboardUtils
                            .getEntrySuffix(i)), null);
                }
                continue;
            }
            String entry = topEntries.get(i);
            int score = sidebarObjective.getScore(entry);
            WrappedTeam team = getEntryTeam(entry);
            if (team == null)
                team = new WrappedTeam("");
            if (!force && entry.equals(line.rawEntry) && (bridge.useComponents() ? (team
                    .getPrefixComp() == line.rawPrefixComp && team.getSuffixComp() == line.rawSuffixComp) : (team
                    .getPrefix().equals(line.rawPrefix) && team.getSuffix().equals(line.rawSuffix)))) {
                if (line.score == null || line.score != score)
                    bridge.updateEntryScore(line.entry + ScoreboardUtils.getEntrySuffix(i), score);
                continue;
            }
            LanguageParser parser = Triton.get().getLanguageParser();
            if (!bridge.useComponents()) {
                String text = parser.scoreboardComponentToString(parser
                        .removeDummyColors(parser.toScoreboardComponents(team.getPrefix() + entry + team.getSuffix())));
                String[] translated = parser.toPacketFormatting(parser.toScoreboardComponents(parser
                        .replaceLanguages(text, owner, Triton.get().getConf().getScoreboardSyntax())));
                if (translated[1].length() > 36)
                    translated[1] = translated[1].substring(0, translated[1].length() - 4);
                if (!translated[1].equals(line.entry) || line.score == null || line.score != score) {
                    toRemove.add(line.entry + ScoreboardUtils.getEntrySuffix(i));
                    doNotRemove.add(translated[1] + ScoreboardUtils.getEntrySuffix(i));
                    bridge.updateEntryScore(translated[1] + ScoreboardUtils.getEntrySuffix(i), score);
                }
                if (!translated[1].equals(line.entry))
                    bridge.addEntryToTeam(translated[1] + ScoreboardUtils.getEntrySuffix(i), i);
                if (!translated[0].equals(line.prefix) || !translated[2].equals(line.suffix))
                    bridge.updateTeamPrefixSuffix(translated[0], translated[2], i);
                line.rawPrefix = team.getPrefix();
                line.rawEntry = entry;
                line.rawSuffix = team.getSuffix();
                line.prefix = translated[0];
                line.entry = translated[1];
                line.suffix = translated[2];
                line.score = score;
            } else {
                String text = parser.scoreboardComponentToString(parser
                        .removeDummyColors(parser.toScoreboardComponents(team.getPrefix() + entry + team.getSuffix())));
                text = parser.replaceLanguages(text, owner, Triton.get().getConf().getScoreboardSyntax());
                if (line.score == null || line.score != score)
                    bridge.updateEntryScore(ScoreboardUtils.getEntrySuffix(i), score);
                bridge.updateTeamPrefixSuffix(ComponentSerializer
                        .toString(TextComponent.fromLegacyText(text)), "{\"text\":\"\"}", i);
                line.rawPrefixComp = team.getPrefixComp();
                line.rawEntry = entry;
                line.rawSuffixComp = team.getSuffixComp();
                line.entry = ScoreboardUtils.getEntrySuffix(i);
                line.score = score;
            }
        }
        if (!bridge.useComponents())
            for (String remove : toRemove)
                if (!doNotRemove.contains(remove))
                    bridge.updateEntryScore(remove, null);
    }

    public WrappedObjective getObjective(String name) {
        for (WrappedObjective obj : objectives)
            if (obj.getName().equals(name)) return obj;
        return null;
    }

    public List<WrappedObjective> getObjectives() {
        return objectives;
    }

    public WrappedObjective createObjective(String name) {
        WrappedObjective obj = new WrappedObjective(name);
        objectives.add(obj);
        return obj;
    }

    public void removeObjective(String name) {
        objectives.remove(getObjective(name));
    }

    public WrappedTeam getTeam(String name) {
        for (WrappedTeam team : teams)
            if (team.getName().equals(name)) return team;
        return null;
    }

    public WrappedTeam createTeam(String name) {
        WrappedTeam team = new WrappedTeam(name);
        teams.add(team);
        return team;
    }

    public void removeTeam(String name) {
        teams.remove(getTeam(name));
    }

    public void setSidebarObjective(WrappedObjective objective) {
        if (!initialized && objective != null) {
            initialized = true;
            bridge.initializeScoreboard(bridge.useComponents() ? ComponentSerializer
                    .toString(Triton.get().getLanguageParser()
                            .parseComponent(owner, Triton.get().getConf().getScoreboardSyntax(), objective
                                    .getTitleComp())) : translate(owner, objective.getTitle(), 32, Triton.get()
                    .getConf().getScoreboardSyntax()));
        }
        this.sidebarObjective = objective;
    }

    private WrappedTeam getEntryTeam(String entry) {
        for (int i = teams.size() - 1; i >= 0; i--) {
            WrappedTeam team = teams.get(i);
            if (team.hasEntry(entry)) return team;
        }
        return null;
    }

    public ScoreboardBridge getBridge() {
        return bridge;
    }

    private String translate(LanguagePlayer lp, String s, int max, MainConfig.FeatureSyntax syntax) {
        String r = Triton.get().getLanguageParser().replaceLanguages(s, lp, syntax);
        if (r.length() > max) return r.substring(0, max);
        return r;
    }
}
