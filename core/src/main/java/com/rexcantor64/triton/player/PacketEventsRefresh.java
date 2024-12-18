package com.rexcantor64.triton.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class PacketEventsRefresh {
    private final Map<String, ScoreBoardTeamInfo> teamsMap = new ConcurrentHashMap<>();

    private final TritonLanguagePlayer<?> languagePlayer;

    /**
     * Stores info of a team for later (i.e., to refresh text when language is changed).
     * The team will NOT be copied, so the caller is responsible for making sure the data
     * is not mutated afterwards.
     *
     * @param teamName The name of the team the info belongs to.
     * @param teamInfo The info of the team.
     * @since 4.0.0
     */
    public void saveScoreboardTeam(String teamName, ScoreBoardTeamInfo teamInfo) {
        teamsMap.put(teamName, teamInfo);
    }

    /**
     * Forget info about the given team.
     *
     * @param teamName The name of the team to forget.
     * @since 4.0.0
     */
    public void discardScoreboardTeam(String teamName) {
        teamsMap.remove(teamName);
    }

    /**
     * Refresh all active features of a player.
     *
     * @since 4.0.0
     */
    public void refreshAll() {
        val playerOpt = this.languagePlayer.getPlatformPlayer();
        if (!playerOpt.isPresent()) {
            return;
        }
        val player = playerOpt.get();
        val user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        val parser = Triton.get().getMessageParser();
        val cfg = Triton.get().getConfig();

        if (cfg.isScoreboards()) {
            updateScoreboardTeams(user, cfg.getScoreboardSyntax(), parser);
        }
    }

    private void updateScoreboardTeams(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull AdventureParser parser) {
        for (val entry : teamsMap.entrySet()) {
            val info = entry.getValue();
            val infoCopy = new ScoreBoardTeamInfo(
                    info.getDisplayName(),
                    info.getPrefix(),
                    info.getSuffix(),
                    info.getTagVisibility(),
                    info.getCollisionRule(),
                    info.getColor(),
                    info.getOptionData()
            );

            // displayName
            parser.translateComponent(
                            infoCopy.getDisplayName(),
                            languagePlayer,
                            syntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(infoCopy::setDisplayName);
            // prefix
            parser.translateComponent(
                            infoCopy.getPrefix(),
                            languagePlayer,
                            syntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(infoCopy::setPrefix);
            // suffix
            parser.translateComponent(
                            infoCopy.getSuffix(),
                            languagePlayer,
                            syntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(infoCopy::setSuffix);

            val packet = new WrapperPlayServerTeams(entry.getKey(), WrapperPlayServerTeams.TeamMode.UPDATE, infoCopy);
            user.sendPacketSilently(packet);
        }
    }
}
