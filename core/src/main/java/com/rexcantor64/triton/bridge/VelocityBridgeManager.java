package com.rexcantor64.triton.bridge;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.player.VelocityLanguagePlayer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class VelocityBridgeManager {
    private final Map<RegisteredServer, Queue<byte[]>> queue = new HashMap<>();

    public void sendPlayerLanguage(@NonNull VelocityLanguagePlayer lp) {
        lp.getParent().getCurrentServer().ifPresent(server -> sendPlayerLanguage(lp, server.getServer()));
    }

    public void sendPlayerLanguage(@NonNull VelocityLanguagePlayer lp, @NonNull RegisteredServer server) {
        val out = BridgeSerializer.buildPlayerLanguageData(lp);
        sendPluginMessage(server, out);
    }

    public void sendExecutableCommand(String command, @NonNull RegisteredServer server) {
        val out = BridgeSerializer.buildExecutableCommandData(command);
        sendPluginMessage(server, out);
    }

    public void sendConfigToEveryone() {
        Triton.get().getLogger().logInfo(2, "Sending config and translations to all Spigot servers...");
        try {
            val languageOut = BridgeSerializer.getLanguageDataOutput();

            // Send language files
            for (val info : Triton.asVelocity().getLoader().getServer().getAllServers())
                sendConfigToServer(info, languageOut);
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError("Failed to send config and language items to other servers! Not everything might work " +
                            "as expected! Error: %1", e.getMessage());
            if (Triton.get().getConf().getLogLevel() > 0)
                e.printStackTrace();
        }
    }

    public void sendConfigToServer(@NonNull RegisteredServer info, byte[] languageOut) {
        Triton.get().getLogger()
                .logInfo(2, "Sending config and translations to '%1' server...", info.getServerInfo().getName());

        if (languageOut == null) languageOut = BridgeSerializer.getLanguageDataOutput();

        for (val message : BridgeSerializer.buildTranslationData(info.getServerInfo().getName(), languageOut)) {
            sendPluginMessage(info, message);
        }

    }

    public void forwardCommand(CommandEvent commandEvent) {
        val out = BridgeSerializer.buildForwardCommandData(commandEvent);

        Triton.asVelocity().getLoader().getServer().getPlayer(commandEvent.getSender().getUUID())
                .flatMap(Player::getCurrentServer)
                .ifPresent(serverConnection -> sendPluginMessage(serverConnection.getServer(), out));
    }

    private void sendPluginMessage(@NonNull RegisteredServer info, byte[] data) {
        if (!info.sendPluginMessage(Triton.asVelocity().getBridgeChannelIdentifier(), data)) {
            queue.computeIfAbsent(info, server -> new LinkedList<>()).add(data);
        }
    }

    public void executeQueue(@NonNull RegisteredServer server) {
        byte[] data;
        val queue = this.queue.get(server);
        if (queue == null) return;

        while ((data = queue.poll()) != null) {
            server.sendPluginMessage(Triton.asVelocity().getBridgeChannelIdentifier(), data);
        }
    }
}
