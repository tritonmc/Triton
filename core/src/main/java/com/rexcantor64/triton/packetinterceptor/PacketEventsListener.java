package com.rexcantor64.triton.packetinterceptor;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.packetinterceptor.handlers.ActionBarPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.BossBarPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ChatPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.DeathScreenPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.DisconnectPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.GuiPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ResourcePackPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.ScoreboardPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.TabPacketHandler;
import com.rexcantor64.triton.packetinterceptor.handlers.TitlePacketHandler;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.val;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Main entrypoint for intercepting packets with PacketEvents.
 * <p>
 * The {@link PacketEventsListener#setupHandlers()} function is NOT
 * called on constructor and thus needs to be called separately.
 *
 * @since 4.0.0
 */
public class PacketEventsListener implements PacketListener {

    private Map<PacketTypeCommon, BiConsumer<PacketSendEvent, TritonLanguagePlayer<?>>> receiveHandlers = Collections.emptyMap();

    /**
     * Setup handlers according to what is enabled on config.
     *
     * @since 4.0.0
     */
    public void setupHandlers() {
        val parser = Triton.get().getMessageParser();
        val config = Triton.get().getConfig();
        val updatedHandlers = new HashMap<PacketTypeCommon, BiConsumer<PacketSendEvent, TritonLanguagePlayer<?>>>();

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
            updatedHandlers.put(PacketType.Play.Server.SYSTEM_CHAT_MESSAGE, chatHandler::onSystemChatMessagePacket);
        }
        if (config.isKick()) {
            val disconnectHandler = new DisconnectPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.DISCONNECT, disconnectHandler::onDisconnectPacket);
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
        if (config.isTab()) {
            val tabHandler = new TabPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER, tabHandler::onPlayerListHeaderAndFooterPacket);
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
        if (config.isGuis()) {
            val guiHandler = new GuiPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.OPEN_WINDOW, guiHandler::onOpenWindowPacket);
        }

        receiveHandlers = Collections.unmodifiableMap(updatedHandlers);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        val type = event.getPacketType();

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
        if (uuid != null) {
            Triton.get().getPlayerManager().unregisterPlayer(uuid);
        }
    }
}
