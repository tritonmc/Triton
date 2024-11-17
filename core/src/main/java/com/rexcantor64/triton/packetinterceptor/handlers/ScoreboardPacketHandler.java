package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.player.LanguagePlayer;
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

    public void onTeamsPacket(@NotNull PacketSendEvent event, @NotNull LanguagePlayer languagePlayer) {
        WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);

        val action = teams.getTeamMode();
        if (action == WrapperPlayServerTeams.TeamMode.REMOVE) {
            // TODO remove from cache
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

        // display name
        parser.translateComponent(
                        info.getDisplayName(),
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
                        info.getPrefix(),
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
                        info.getSuffix(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setSuffix(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            // TODO save data to cache
        } else {
            // TODO remove team from cache if exists
        }
    }

}
