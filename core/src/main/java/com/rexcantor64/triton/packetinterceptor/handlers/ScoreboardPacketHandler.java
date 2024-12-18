package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ScoreboardPacketHandler {

    private final @NotNull AdventureParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public ScoreboardPacketHandler(@NotNull AdventureParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getScoreboardSyntax();
    }

    public void onTeamsPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);

        val action = teams.getTeamMode();
        if (action == WrapperPlayServerTeams.TeamMode.REMOVE) {
            languagePlayer.getPacketEventsRefresh().discardScoreboardTeam(teams.getTeamName());
        }

        if (action != WrapperPlayServerTeams.TeamMode.CREATE && action != WrapperPlayServerTeams.TeamMode.UPDATE) {
            // we are only interested in new/update actions
            return;
        }

        val infoOpt = teams.getTeamInfo();
        if (!infoOpt.isPresent()) {
            // this should never happen since we have filtered by the actions before
            return;
        }
        val info = infoOpt.get();

        val originalDisplayName = info.getDisplayName();
        val originalPrefix = info.getPrefix();
        val originalSuffix = info.getSuffix();

        // display name
        parser.translateComponent(
                        originalDisplayName,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setDisplayName(result);
                    event.markForReEncode(true);
                });
        // prefix
        parser.translateComponent(
                        originalPrefix,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setPrefix(result);
                    event.markForReEncode(true);
                });
        // suffix
        parser.translateComponent(
                        originalSuffix,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setSuffix(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            val teamInfoCopy = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    originalDisplayName,
                    originalPrefix,
                    originalSuffix,
                    info.getTagVisibility(),
                    info.getCollisionRule(),
                    info.getColor(),
                    info.getOptionData()
            );

            languagePlayer.getPacketEventsRefresh().saveScoreboardTeam(teams.getTeamName(), teamInfoCopy);
        } else {
            languagePlayer.getPacketEventsRefresh().discardScoreboardTeam(teams.getTeamName());
        }
    }

    public void onObjectivePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerScoreboardObjective(event);

        val action = packet.getMode();
        if (action == WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE) {
            languagePlayer.getPacketEventsRefresh().discardScoreboardObjective(packet.getName());
        }

        if (action != WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE && action != WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE) {
            // we are only interested in new/update actions
            return;
        }

        val originalDisplayName = packet.getDisplayName();

        parser.translateComponent(
                        originalDisplayName,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setDisplayName(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            languagePlayer.getPacketEventsRefresh().saveScoreboardObjective(
                    packet.getName(),
                    originalDisplayName,
                    packet.getRenderType(),
                    packet.getScoreFormat()
            );
        } else {
            languagePlayer.getPacketEventsRefresh().discardScoreboardObjective(packet.getName());
        }
    }

}
