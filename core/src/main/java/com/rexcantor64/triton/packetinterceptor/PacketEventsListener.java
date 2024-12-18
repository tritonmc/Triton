package com.rexcantor64.triton.packetinterceptor;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.packetinterceptor.handlers.ScoreboardPacketHandler;
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

        if (config.isScoreboards()) {
            val scoreboardHandler = new ScoreboardPacketHandler(parser, config);
            updatedHandlers.put(PacketType.Play.Server.TEAMS, scoreboardHandler::onTeamsPacket);
            updatedHandlers.put(PacketType.Play.Server.SCOREBOARD_OBJECTIVE, scoreboardHandler::onObjectivePacket);
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
}
