package com.rexcantor64.triton.packetinterceptor;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.packetinterceptor.handlers.ScoreboardPacketHandler;
import lombok.val;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Main entrypoint for intercepting packets with PacketEvents.
 * <p>
 * The {@link PacketEventsListener#setupHandlers()} function is NOT
 * called on constructor and thus needs to be called separately.
 *
 * @since 4.0.0
 */
public class PacketEventsListener implements PacketListener {

    private Map<PacketTypeCommon, Consumer<PacketSendEvent>> receiveHandlers = Collections.emptyMap();

    /**
     * Setup handlers according to what is enabled on config.
     *
     * @since 4.0.0
     */
    public void setupHandlers() {
        val config = Triton.get().getConfig();
        val updatedHandlers = new HashMap<PacketTypeCommon, Consumer<PacketSendEvent>>();

        if (config.isScoreboards()) {
            val scoreboardHandler = new ScoreboardPacketHandler();
            updatedHandlers.put(PacketType.Play.Server.TEAMS, scoreboardHandler::onTeamsPacket);
        }

        receiveHandlers = Collections.unmodifiableMap(updatedHandlers);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        val type = event.getPacketType();
        System.out.println("type = " + type);

        val handler = receiveHandlers.get(type);
        if (handler != null) {
            handler.accept(event);
        }
    }
}
