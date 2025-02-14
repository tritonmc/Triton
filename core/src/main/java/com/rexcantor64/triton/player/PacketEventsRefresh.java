package com.rexcantor64.triton.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.RenderType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PacketEventsRefresh {
    private final Map<UUID, Component> bossBarMap = new ConcurrentHashMap<>();
    private final Map<String, ScoreBoardTeamInfo> teamsMap = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardObjective> objectivesMap = new ConcurrentHashMap<>();
    private @Nullable PacketEventsRefresh.PlayerListHeaderFooter playerListHeaderFooter;
    private final Map<UUID, Component> playerInfoMap = new ConcurrentHashMap<>();

    private final TritonLanguagePlayer<?> languagePlayer;

    /**
     * Stores text of boss bar for later (i.e., to refresh text when language is changed).
     *
     * @param uuid The UUID of the boss bar.
     * @param text The text of the boss bar.
     * @since 4.0.0
     */
    public void saveBossBar(@NotNull UUID uuid, @NotNull Component text) {
        this.bossBarMap.put(uuid, text);
    }

    /**
     * Forget info about the given boss bar.
     *
     * @param uuid The UUID of the player.
     * @since 4.0.0
     */
    public void discardBossBar(@NotNull UUID uuid) {
        this.bossBarMap.remove(uuid);
    }

    /**
     * Forget about all boss bars.
     *
     * @since 4.0.0
     */
    public void discardAllBossBars() {
        this.bossBarMap.clear();
    }

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
     * Stores info of an objective for later (i.e., to refresh text when language is changed).
     *
     * @param objectiveName The name of the objective the info belongs to.
     * @param displayName   The untranslated display name of the team.
     * @param renderType    The last render type sent with the packet.
     * @param scoreFormat   The last score format sent with the packet.
     * @since 4.0.0
     */
    public void saveScoreboardObjective(@NotNull String objectiveName,
                                        @NotNull Component displayName,
                                        @Nullable RenderType renderType,
                                        @Nullable ScoreFormat scoreFormat) {
        val info = new ScoreboardObjective(displayName, renderType, scoreFormat);
        objectivesMap.put(objectiveName, info);
    }

    /**
     * Forget info about the given objective.
     *
     * @param objectiveName The name of the objective to forget.
     * @since 4.0.0
     */
    public void discardScoreboardObjective(String objectiveName) {
        objectivesMap.remove(objectiveName);
    }

    /**
     * Stores info of the header/footer of the player list for later (i.e., to refresh text when language is changed).
     *
     * @param header The header text.
     * @param footer The footer text.
     * @since 4.0.0
     */
    public void savePlayerListHeaderFooter(@NotNull Component header, @NotNull Component footer) {
        this.playerListHeaderFooter = new PlayerListHeaderFooter(header, footer);
    }

    /**
     * Forget info about the header/footer of the player list.
     *
     * @since 4.0.0
     */
    public void discardPlayerListHeaderFooter() {
        this.playerListHeaderFooter = null;
    }

    /**
     * Stores info of the player list's entry for later (i.e., to refresh text when language is changed).
     *
     * @param uuid The UUID of the player.
     * @param name The display name of the player.
     * @since 4.0.0
     */
    public void savePlayerInfo(@NotNull UUID uuid, @NotNull Component name) {
        this.playerInfoMap.put(uuid, name);
    }

    /**
     * Forget info about the given entry in the player list.
     *
     * @param uuid The UUID of the player.
     * @since 4.0.0
     */
    public void discardPlayerInfo(@NotNull UUID uuid) {
        this.playerInfoMap.remove(uuid);
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

        if (cfg.isBossbars()) {
            val syntax = cfg.getBossbarSyntax();
            updateBossBars(user, syntax, parser);
        }
        if (cfg.isScoreboards()) {
            val syntax = cfg.getScoreboardSyntax();
            updateScoreboardTeams(user, syntax, parser);
            updateScoreboardObjectives(user, syntax, parser);
        }
        if (cfg.isTab()) {
            val syntax = cfg.getTabSyntax();
            updatePlayerListHeaderFooter(user, syntax, parser);
            updatePlayerList(user, syntax, parser);
        }
    }

    private void updateBossBars(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull AdventureParser parser) {
        for (val entry : bossBarMap.entrySet()) {
            val packet = new WrapperPlayServerBossBar(entry.getKey(), WrapperPlayServerBossBar.Action.UPDATE_TITLE);
            parser.translateComponent(
                            entry.getValue(),
                            languagePlayer,
                            syntax
                    )
                    .ifUnchanged(() -> packet.setTitle(Component.empty())) // unreachable
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(packet::setTitle);
            user.sendPacketSilently(packet);
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

    private void updateScoreboardObjectives(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull AdventureParser parser) {
        for (val entry : objectivesMap.entrySet()) {
            val info = entry.getValue();

            val packet = new WrapperPlayServerScoreboardObjective(
                    entry.getKey(),
                    WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE,
                    info.getDisplayName(),
                    info.getRenderType(),
                    info.getScoreFormat()
            );

            parser.translateComponent(
                            packet.getDisplayName(),
                            languagePlayer,
                            syntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(packet::setDisplayName);

            user.sendPacketSilently(packet);
        }
    }

    private void updatePlayerListHeaderFooter(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull AdventureParser parser) {
        if (this.playerListHeaderFooter == null) {
            return;
        }

        val packet = new WrapperPlayServerPlayerListHeaderAndFooter(
                this.playerListHeaderFooter.getHeader(),
                this.playerListHeaderFooter.getFooter()
        );

        parser.translateComponent(
                        packet.getHeader(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(packet::setHeader);
        parser.translateComponent(
                        packet.getFooter(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(packet::setFooter);

        user.sendPacketSilently(packet);
    }

    private void updatePlayerList(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull AdventureParser parser) {
        if (user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            val packet = new WrapperPlayServerPlayerInfoUpdate(
                    EnumSet.of(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME),
                    this.playerInfoMap.entrySet().stream()
                            .map(entry -> {
                                val info = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(entry.getKey());
                                parser.translateComponent(
                                                entry.getValue(),
                                                languagePlayer,
                                                syntax
                                        )
                                        .getResultOrToRemove(() -> null)
                                        .ifPresent(info::setDisplayName);
                                return info;
                            })
                            .collect(Collectors.toList())
            );
            user.sendPacketSilently(packet);
        } else {
            val packet = new WrapperPlayServerPlayerInfo(
                    WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME,
                    this.playerInfoMap.entrySet().stream()
                            .map(entry -> {
                                val info = new WrapperPlayServerPlayerInfo.PlayerData(null, new UserProfile(entry.getKey(), ""), null, 0);
                                parser.translateComponent(
                                                entry.getValue(),
                                                languagePlayer,
                                                syntax
                                        )
                                        .getResultOrToRemove(() -> null)
                                        .ifPresent(info::setDisplayName);
                                return info;
                            })
                            .collect(Collectors.toList())
            );
            user.sendPacketSilently(packet);
        }
    }

    @RequiredArgsConstructor
    @Getter
    private static class ScoreboardObjective {
        private final Component displayName;
        private final RenderType renderType;
        private final ScoreFormat scoreFormat;
    }

    @RequiredArgsConstructor
    @Getter
    private static class PlayerListHeaderFooter {
        private final @NotNull Component header;
        private final @NotNull Component footer;
    }
}
