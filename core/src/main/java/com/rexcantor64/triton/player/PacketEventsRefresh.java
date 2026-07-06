package com.rexcantor64.triton.player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective.RenderType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.ScoreBoardTeamInfo;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class PacketEventsRefresh {
    private final Map<UUID, Component> bossBarMap = new ConcurrentHashMap<>();
    private final Map<String, ScoreBoardTeamInfo> teamsMap = new ConcurrentHashMap<>();
    private final Map<String, ScoreboardObjective> objectivesMap = new ConcurrentHashMap<>();
    private @Nullable PacketEventsRefresh.PlayerListHeaderFooter playerListHeaderFooter;
    private final Map<UUID, Component> playerInfoMap = new ConcurrentHashMap<>();
    private final Map<Integer, Entity> entityMap = new ConcurrentHashMap<>();
    private final Map<UUID, Player> tentativePlayerMap = new ConcurrentHashMap<>();
    private final Map<Integer, Player> spawnedPlayerMap = new ConcurrentHashMap<>();

    /**
     * The window ID of the currently open window.
     * If no window is open, this must be set to 0, which corresponds to the player's inventory.
     */
    @Setter
    @Getter
    private int currentWindowId = 0;

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
     * Store info about an entity, even if there is nothing to translate at the moment.
     * This signals that this entity is of a type that should be translated.
     *
     * @param id The ID of the entity (from the perspective of the player).
     * @since 4.0.0
     */
    public void saveEntity(int id) {
        this.entityMap.put(id, new Entity());
    }

    /**
     * Get info about entity given its ID.
     * The entity only exists if it is of a type that should be translated.
     *
     * @param id The ID of the entity (from the perspective of the player).
     * @return The entity, if it exists and should be translated. Otherwise, the Optional is empty.
     * @since 4.0.0
     */
    public @NotNull Optional<Entity> getEntity(int id) {
        return Optional.ofNullable(this.entityMap.get(id));
    }

    /**
     * Forget an entity, presumably because it has been despawned.
     *
     * @param id The ID of the entity (from the perspective of the player).
     * @since 4.0.0
     */
    public void discardEntity(int id) {
        this.entityMap.remove(id);
    }

    /**
     * Forget all entities, including players.
     *
     * @since 4.0.0
     */
    public void discardAllEntitiesAndPlayers() {
        this.entityMap.clear();
        this.spawnedPlayerMap.clear();
    }

    /**
     * Save a player that has been registered (through the player info packet), but hasn't been spawned yet.
     * Only players whose names contain placeholders (e.g., NPCs) should be stored.
     *
     * @param uuid The UUID of the player entity.
     * @param name The original name of the player entity.
     * @return The newly-created instance of the player state.
     * @since 4.0.0
     */
    @Contract("_, _ -> new")
    public @NotNull Player saveTentativePlayer(@NotNull UUID uuid, @NotNull String name) {
        val player = new Player(uuid, name);
        this.tentativePlayerMap.put(uuid, player);
        return player;
    }

    /**
     * Get a player state from their UUID. Keep in mind this is likely not a real player, but an NPC.
     *
     * @param uuid The UUID of the player entity.
     * @return The player state matching the given UUID, if it exists.
     * @since 4.0.0
     */
    public @NotNull Optional<Player> getTentativePlayer(@NotNull UUID uuid) {
        return Optional.ofNullable(this.tentativePlayerMap.get(uuid));
    }

    /**
     * Discard a player state when it is unregistered (i.e., through the player info packet).
     * It is possible that references to the {@link Player} object still exist in the spawned players map.
     *
     * @param uuid The UUID of the player entity.
     * @since 4.0.0
     */
    public void discardTentativePlayer(@NotNull UUID uuid) {
        this.tentativePlayerMap.remove(uuid);
    }

    /**
     * Associate a player state ({@link Player}) with a given entity ID when it is spawned.
     * The player state can be retrieved from {@link PacketEventsRefresh#getTentativePlayer(UUID)}.
     *
     * @param id     The ID of the spawned entity (from the perspective of the current player).
     * @param player The player state, for refresh purposes.
     * @since 4.0.0
     */
    public void saveSpawnedPlayer(int id, @NotNull Player player) {
        this.spawnedPlayerMap.put(id, player);
    }

    /**
     * Get a player state from their entity ID. Keep in mind this is likely not a real player, but an NPC.
     *
     * @param id The ID of the player entity (from the perspective of the current player).
     * @return The player state matching the given entity ID, if it exists.
     * @since 4.0.0
     */
    public @NotNull Optional<Player> getSpawnedPlayer(int id) {
        return Optional.ofNullable(this.spawnedPlayerMap.get(id));
    }

    /**
     * Discard a player state when it is despawned.
     * It is possible that references to the {@link Player} object still exist in the tentative players map.
     *
     * @param id The ID of the player entity (from the perspective of the current player).
     * @since 4.0.0
     */
    public void discardSpawnedPlayer(int id) {
        this.spawnedPlayerMap.remove(id);
    }

    /**
     * Refresh all active features of a player.
     *
     * @since 4.0.0
     */
    public void refreshAll() {
        val playerOpt = this.languagePlayer.getPlatformPlayer();
        if (playerOpt.isEmpty()) {
            Triton.get().getLogger().logDebug(
                    "Skipped refreshing features for player %1 because could not get the platform's player instance",
                    this.languagePlayer.getUUID()
            );
            return;
        }
        val player = playerOpt.get();
        val user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        val parser = Triton.get().getMessageParser();
        val cfg = Triton.get().getConfig();

        if (user == null) {
            Triton.get().getLogger().logDebug(
                    "Skipped refreshing features for player %1 because could not get a PacketEvents user instance",
                    this.languagePlayer.getUUID()
            );
            return;
        }

        if (user.getEncoderState() != ConnectionState.PLAY) {
            // We cannot send these packets if the connection is not in the PLAY state.
            // Doing so causes the client to throw an error and disconnect.
            Triton.get().getLogger().logDebug(
                    "Skipped refreshing features for player %1 because the connection is not in the play phase",
                    this.languagePlayer.getUUID()
            );
            return;
        }

        if (cfg.isBossbars()) {
            val syntax = cfg.getBossbarSyntax();
            updateBossBars(user, syntax, parser);
        }
        if (cfg.isScoreboards()) {
            val syntax = cfg.getScoreboardSyntax();
            updateScoreboardTeams(user, syntax, parser);
            updateScoreboardObjectives(user, syntax, parser);
        }
        if (cfg.isHologramsAll() || !cfg.getHolograms().isEmpty()) {
            val syntax = cfg.getHologramSyntax();
            updateEntities(user, syntax, parser);
            // translate players before tab, so that the custom display name can be translated
            // by the tab refresher
            updatePlayer(user, syntax, parser);
        }
        if (cfg.isTab()) {
            val syntax = cfg.getTabSyntax();
            updatePlayerListHeaderFooter(user, syntax, parser);
            updatePlayerList(user, syntax, parser);
        }
        if (cfg.isItems()) {
            updateInventoryItems(user);
        }
    }

    private void updateBossBars(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
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

    private void updateScoreboardTeams(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
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

    private void updateScoreboardObjectives(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
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

    private void updatePlayerListHeaderFooter(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
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

    private void updatePlayerList(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
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

    private void updateEntities(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
        for (val entry : entityMap.entrySet()) {
            val entity = entry.getValue();
            val entityData = new ArrayList<EntityData<?>>();
            val displayName = entity.getDisplayName();
            if (displayName != null) {
                val showCustomName = new AtomicBoolean(entity.isShowDisplayName());
                Optional<Component> result = parser.translateComponent(
                                displayName,
                                languagePlayer,
                                syntax
                        )
                        .mapToObj(
                                Optional::ofNullable,
                                () -> Optional.of(displayName),
                                () -> {
                                    showCustomName.set(false);
                                    return Optional.empty();
                                }
                        );
                entityData.add(new EntityData<>(2, EntityDataTypes.OPTIONAL_ADV_COMPONENT, result));
                entityData.add(new EntityData<>(3, EntityDataTypes.BOOLEAN, showCustomName.get()));
            }
            val textDisplayContent = entity.getTextDisplayContent();
            if (textDisplayContent != null) {
                val result = parser.translateComponent(
                                textDisplayContent,
                                languagePlayer,
                                syntax
                        )
                        .getResultOrToRemove(Component::empty)
                        .orElse(textDisplayContent);
                val id = user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_2) ? 23 : 22;
                entityData.add(new EntityData<>(id, EntityDataTypes.ADV_COMPONENT, result));
            }

            if (entityData.isEmpty()) {
                continue;
            }
            val packet = new WrapperPlayServerEntityMetadata(entry.getKey(), entityData);
            user.sendPacketSilently(packet);
        }
    }

    private void updatePlayer(@NotNull User user, @NotNull MainConfig.FeatureSyntax syntax, @NotNull MessageParser parser) {
        for (val entry : this.spawnedPlayerMap.entrySet()) {
            // Changing the name of a player entity requires 4 steps:
            // - remove player info
            // - despawn entity
            // - send player info
            // - spawn entity
            // This means that most of the properties of a player will be discarded by the client,
            // so Triton must send them again.
            // It is unrealistic for Triton to keep track of all these properties, so these are provided
            // on a best-effort basis.
            // Server owners should use holograms (either text displays, or legacy armor-stands) instead.
            val id = entry.getKey();
            val player = entry.getValue();

            val removePlayerInfoPacket = getRemovePlayerInfoPacket(user.getClientVersion(), player.getUuid());
            val despawnEntity = new WrapperPlayServerDestroyEntities(id);

            val translatedName = parser.translateString(
                            player.getName(),
                            languagePlayer,
                            syntax
                    )
                    .mapToObj(
                            Function.identity(),
                            player::getName,
                            String::new
                    );
            val addPlayerInfoPacket = getAddPlayerInfoPacket(user.getClientVersion(), player, translatedName);
            val spawnEntityPacket = getSpawnPlayerPacket(user.getClientVersion(), id, player);

            user.sendPacketSilently(removePlayerInfoPacket);
            user.sendPacketSilently(despawnEntity);
            user.sendPacketSilently(addPlayerInfoPacket);
            user.sendPacketSilently(spawnEntityPacket);
            if (user.getClientVersion().isOlderThan(ClientVersion.V_1_20_2)) {
                // before 1.20.2, headYaw was not sent on the spawn packet, so it has to be sent separately
                val headRotationPacket = new WrapperPlayServerEntityHeadLook(id, player.getHeadYaw());
                user.sendPacketSilently(headRotationPacket);
            }
        }
    }

    private @NotNull PacketWrapper<?> getRemovePlayerInfoPacket(ClientVersion clientVersion, UUID uuid) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            return new WrapperPlayServerPlayerInfoRemove(uuid);
        }
        val playerInfo = new WrapperPlayServerPlayerInfo.PlayerData(null, new UserProfile(uuid, null), null, 0);
        return new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER, playerInfo);
    }

    private @NotNull PacketWrapper<?> getAddPlayerInfoPacket(ClientVersion clientVersion, Player player, String translatedName) {
        val userProfile = new UserProfile(player.getUuid(), translatedName, player.getTextureProperties());
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_19_3)) {
            val actionList = EnumSet.of(
                    WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER
            );
            val playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(userProfile);
            if (player.getDisplayName() != null) {
                actionList.add(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME);
                playerInfo.setDisplayName(playerInfo.getDisplayName());
            }
            if (player.getLatency() != null) {
                actionList.add(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY);
                playerInfo.setLatency(player.getLatency());
            }
            if (player.getListed() != null) {
                actionList.add(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED);
                playerInfo.setListed(player.getListed());
            }
            if (player.getListOrder() != null) {
                actionList.add(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LIST_ORDER);
                playerInfo.setListOrder(player.getListOrder());
            }
            if (player.getShowHat() != null) {
                actionList.add(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_HAT);
                playerInfo.setShowHat(player.getShowHat());
            }
            return new WrapperPlayServerPlayerInfoUpdate(actionList, playerInfo);
        }
        val latency = player.getLatency();
        val playerInfo = new WrapperPlayServerPlayerInfo.PlayerData(
                player.getDisplayName(),
                userProfile,
                GameMode.SURVIVAL,
                latency == null ? 0 : latency
        );
        return new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.ADD_PLAYER, playerInfo);
    }

    private @NotNull PacketWrapper<?> getSpawnPlayerPacket(ClientVersion clientVersion, int id, Player player) {
        if (clientVersion.isNewerThanOrEquals(ClientVersion.V_1_20_2)) {
            return new WrapperPlayServerSpawnEntity(
                    id,
                    Optional.of(player.getUuid()),
                    EntityTypes.PLAYER,
                    player.getPosition(),
                    player.getPitch(),
                    player.getYaw(),
                    player.getHeadYaw(),
                    0, // entity data that we can ignore
                    Optional.empty()
            );
        }
        return new WrapperPlayServerSpawnPlayer(
                id,
                player.getUuid(),
                player.getPosition(),
                player.getYaw(),
                player.getPitch(),
                new ArrayList<>()
        );
    }

    private void updateInventoryItems(@NotNull User user) {
        if (Triton.isBungee()) {
            return;
        }
        if (user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_17_1)) {
            // Here we use a hack where we pretend to the server that the client is out-of-sync (i.e., incorrect stateId),
            // making it resend the entire window contents
            val packet = new WrapperPlayClientClickWindow(this.currentWindowId, -1, 0, Byte.MAX_VALUE, WrapperPlayClientClickWindow.WindowClickType.PICKUP, Collections.emptyMap(), Optional.empty());
            user.receivePacketSilently(packet);
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

    /**
     * Stores data about an entity that can be used to refresh when the language of a player changes.
     *
     * @since 4.0.0
     */
    @RequiredArgsConstructor
    @Getter
    @Setter
    public static class Entity {
        private @Nullable Component displayName;
        private boolean showDisplayName;
        private @Nullable Component textDisplayContent;
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    public static class Player {
        private final @NotNull UUID uuid;
        private final @NotNull String name;
        private @NotNull List<TextureProperty> textureProperties = new ArrayList<>();

        // only for refreshes, not to translate
        private @Nullable Component displayName;
        private @Nullable Integer latency;
        private @Nullable Boolean listed;
        private @Nullable Integer listOrder;
        private @Nullable Boolean showHat;

        private @NotNull Vector3d position = Vector3d.zero();
        private float pitch;
        private float yaw;
        private float headYaw;
    }
}
