package com.rexcantor64.triton.packetinterceptor;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.packetinterceptor.handlers.ActionBarPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.BossBarPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ChatPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.DeathScreenPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.DialogPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.DisconnectPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.EntityPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.GuiPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ItemPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ResourcePackPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ScoreboardPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.TabPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.TitlePacketHandler;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.val;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Main entrypoint for intercepting packets with PacketEvents.
 * <p>
 * The {@link PacketEventsListener#setupHandlers()} function is NOT
 * called on constructor and thus needs to be called separately.
 *
 * @since 4.0.0
 */
public class PacketEventsListener implements PacketListener {

    private Map<PacketTypeCommon, BiConsumer<PacketSendEvent, TritonLanguagePlayer<?>>> sendHandlers = Collections.emptyMap();
    private Map<PacketTypeCommon, BiConsumer<PacketReceiveEvent, TritonLanguagePlayer<?>>> receiveHandlers = Collections.emptyMap();

    /**
     * Setup handlers according to what is enabled on config.
     *
     * @since 4.0.0
     */
    public void setupHandlers() {
        val parser = Triton.get().getMessageParser();
        val config = Triton.get().getConfig();
        val updatedHandlers = new HashMap<PacketTypeCommon, BiConsumer<PacketSendEvent, TritonLanguagePlayer<?>>>();
        val updatedReceiveHandlers = new HashMap<PacketTypeCommon, BiConsumer<PacketReceiveEvent, TritonLanguagePlayer<?>>>();

        // parse allowed entity types from config
        val allowedEntities = config.getAllowedEntityTypes().stream()
                .map(type -> {
                    String normalisedType = type;
                    if (!type.contains(":")) {
                        normalisedType = "minecraft:" + type.toLowerCase(Locale.ROOT);
                    }
                    val entityType = EntityTypes.getByName(normalisedType);
                    if (entityType == null) {
                        Triton.get().getLogger().logWarning("Could not find entity type '%1', ignoring", type);
                    }
                    return entityType;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        val shouldTranslatePlayers = config.isHologramsAll() || allowedEntities.contains(EntityTypes.PLAYER);

        if (config.isActionbars()) {
            val actionBarHandler = new ActionBarPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.ACTION_BAR, actionBarHandler::onActionBarPacket);
        }
        if (config.isBossbars()) {
            val bossBarHandler = new BossBarPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.BOSS_BAR, bossBarHandler::onBossBarPacket);
        }
        if (config.isChat() || config.isActionbars()) {
            val chatHandler = new ChatPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.CHAT_MESSAGE, chatHandler::onChatMessagePacket);
            updatedHandlers.put(PacketType.Play.Server.DISGUISED_CHAT, chatHandler::onDisguisedChatPacket);
            updatedHandlers.put(PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, chatHandler::onSystemChatMessagePacket);
        }
        if (config.isKick()) {
            val disconnectHandler = new DisconnectPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Login.Server.DISCONNECT, disconnectHandler::onLoginDisconnectPacket);
            updatedHandlers.put(PacketType.Configuration.Server.DISCONNECT, disconnectHandler::onConfigDisconnectPacket);
            updatedHandlers.put(PacketType.Play.Server.DISCONNECT, disconnectHandler::onPlayDisconnectPacket);
        }
        if (config.isResourcePackPrompt()) {
            val resourcePackHandler = new ResourcePackPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.RESOURCE_PACK_SEND, resourcePackHandler::onResourcePackSendPacket);
        }
        if (config.isScoreboards()) {
            val scoreboardHandler = new ScoreboardPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.TEAMS, scoreboardHandler::onTeamsPacket);
            updatedHandlers.put(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, scoreboardHandler::onObjectivePacket);
        }
        if (config.isTab() || shouldTranslatePlayers) {
            val tabHandler = new TabPacketHandler(parser, config, shouldTranslatePlayers);
            if (config.isTab()) {
                updatedHandlers.put(PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER, tabHandler::onPlayerListHeaderAndFooterPacket);
            }
            updatedHandlers.put(PacketType.Play.Server.PLAYER_INFO, tabHandler::onPlayerInfoPacket);
            updatedHandlers.put(PacketType.Play.Server.PLAYER_INFO_UPDATE, tabHandler::onPlayerInfoUpdatePacket);
            updatedHandlers.put(PacketType.Play.Server.PLAYER_INFO_REMOVE, tabHandler::onPlayerInfoRemovePacket);
        }
        if (config.isTitles() || config.isActionbars()) {
            val titleHandler = new TitlePacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.TITLE, titleHandler::onTitlePacket); // this packet also handles actionbar
            if (config.isTitles()) {
                updatedHandlers.put(PacketType.Play.Server.SET_TITLE_TEXT, titleHandler::onSetTitleTextPacket);
                updatedHandlers.put(PacketType.Play.Server.SET_TITLE_SUBTITLE, titleHandler::onSetTitleSubtitlePacket);
            }
        }
        if (config.isDeathScreen()) {
            val deathScreenHandler = new DeathScreenPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.COMBAT_EVENT, deathScreenHandler::onCombatEventPacket);
            updatedHandlers.put(PacketType.Play.Server.DEATH_COMBAT_EVENT, deathScreenHandler::onDeathCombatEventPacket);
        }
        if (config.isGuis() || config.isItems()) {
            val guiHandler = new GuiPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.OPEN_WINDOW, guiHandler::onOpenWindowPacket);
        }
        if (config.isHologramsAll() || !config.getAllowedEntityTypes().isEmpty()) {
            val entityHandler = new EntityPacketHandler(parser, config, allowedEntities);
            updatedHandlers.put(PacketType.Play.Server.SPAWN_ENTITY, entityHandler::onSpawnEntityPacket);
            updatedHandlers.put(PacketType.Play.Server.SPAWN_LIVING_ENTITY, entityHandler::onSpawnLivingEntityPacket);
            updatedHandlers.put(PacketType.Play.Server.SPAWN_PLAYER, entityHandler::onSpawnPlayerPacket);
            updatedHandlers.put(PacketType.Play.Server.DESTROY_ENTITIES, entityHandler::onDestroyEntitiesPacket);
            updatedHandlers.put(PacketType.Play.Server.ENTITY_METADATA, entityHandler::onEntityMetadataPacket);

            if (shouldTranslatePlayers) {
                // for keeping track of players' location only, for refresh
                updatedHandlers.put(PacketType.Play.Server.ENTITY_POSITION_SYNC, entityHandler::onEntityPositionSync);
                updatedHandlers.put(PacketType.Play.Server.ENTITY_TELEPORT, entityHandler::onEntityTeleport);
                updatedHandlers.put(PacketType.Play.Server.ENTITY_RELATIVE_MOVE, entityHandler::onEntityRelativeMove);
                updatedHandlers.put(PacketType.Play.Server.ENTITY_RELATIVE_MOVE_AND_ROTATION, entityHandler::onEntityRelativeMoveAndRotation);
                updatedHandlers.put(PacketType.Play.Server.ENTITY_ROTATION, entityHandler::onEntityRotation);
                updatedHandlers.put(PacketType.Play.Server.ENTITY_HEAD_LOOK, entityHandler::onEntityHeadLook);
            }

            updatedHandlers.put(PacketType.Play.Server.JOIN_GAME, entityHandler::onJoinGame);
        }
        if (config.isItems()) {
            val itemHandler = new ItemPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.SET_CURSOR_ITEM, itemHandler::onSetCursorItemPacket);
            updatedHandlers.put(PacketType.Play.Server.SET_PLAYER_INVENTORY, itemHandler::onSetPlayerInventoryPacket);
            updatedHandlers.put(PacketType.Play.Server.SET_SLOT, itemHandler::onSetSlotPacket);
            updatedHandlers.put(PacketType.Play.Server.WINDOW_ITEMS, itemHandler::onWindowItemsPacket);
            updatedHandlers.put(PacketType.Play.Server.CLOSE_WINDOW, itemHandler::onServerCloseWindowPacket);
            updatedReceiveHandlers.put(PacketType.Play.Client.CLOSE_WINDOW, itemHandler::onClientCloseWindowPacket);
        }
        if (config.isDialogs()) {
            val dialogHandler = new DialogPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Configuration.Server.SHOW_DIALOG, dialogHandler::onConfigShowDialogPacket);
            updatedHandlers.put(PacketType.Play.Server.SHOW_DIALOG, dialogHandler::onPlayShowDialogPacket);
        }

        sendHandlers = Collections.unmodifiableMap(updatedHandlers);
        receiveHandlers = Collections.unmodifiableMap(updatedReceiveHandlers);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        val type = event.getPacketType();

        val handler = sendHandlers.get(type);
        if (handler != null) {
            val uuid = event.getUser().getUUID();
            if (uuid != null) {
                val languagePlayer = Triton.get().getPlayerManager().get(uuid);
                handler.accept(event, languagePlayer);
            }
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        val type = event.getPacketType();

        if (type == PacketType.Login.Client.LOGIN_START) {
            val packet = new WrapperLoginClientLoginStart(event);
            packet.getPlayerUUID().ifPresent(uuid -> {
                event.getUser().getProfile().setUUID(uuid);
                val languagePlayer = Triton.get().getPlayerManager().get(event.getUser().getUUID());
                languagePlayer.increaseConnectionCount();
            });
        }

        val handler = receiveHandlers.get(type);
        if (handler != null) {
            val languagePlayer = Triton.get().getPlayerManager().get(event.getUser().getUUID());
            handler.accept(event, languagePlayer);
        }
    }

    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        // force language player to be unregistered
        val uuid = event.getUser().getUUID();
        val playerManager = Triton.get().getPlayerManager();
        if (uuid != null && playerManager.hasPlayer(uuid)) {
            val lp = playerManager.get(event.getUser().getUUID());
            lp.decreaseConnectionCount();
            if (lp.getConnectionCount() <= 0) {
                playerManager.unregisterPlayer(uuid);
            }
        }
    }
}
