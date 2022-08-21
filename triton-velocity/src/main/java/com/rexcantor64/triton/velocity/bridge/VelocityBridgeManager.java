package com.rexcantor64.triton.velocity.bridge;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.bridge.BridgeSerializer;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.NonNull;
import lombok.val;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

public class VelocityBridgeManager implements BridgeManager {
    private final Map<RegisteredServer, Queue<byte[]>> queue = new HashMap<>();

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().equals(VelocityTriton.asVelocity().getBridgeChannelIdentifier()) ||
                !(e.getSource() instanceof ServerConnection)) return;

        val in = e.dataAsDataStream();

        try {
            val action = in.readByte();

            // Player changes language
            if (action == 0) {
                val uuid = UUID.fromString(in.readUTF());
                val language = in.readUTF();

                val player = VelocityTriton.asVelocity().getPlayerManager().get(uuid);
                if (player != null)
                    Triton.get().runAsync(() -> player
                            .setLang(Triton.get().getLanguageManager().getLanguageByName(language, true), false));
            }

            // Add or remove a location from a sign group using /triton sign
            if (action == 1) {
                val server = ((ServerConnection) e.getSource());
                SignLocation location = new SignLocation(server.getServerInfo().getName(), in.readUTF(), in.readInt(), in.readInt(), in
                        .readInt());

                // Whether we're adding a location to a group or removing one from a group
                boolean add = in.readBoolean();
                val key = add ? in.readUTF() : null;

                val changed = Triton.get().getStorage().toggleLocationForSignGroup(location, key);

                Triton.get().runAsync(() -> {
                    Triton.get().getLogger().logDebug("Saving sign to storage...");
                    Triton.get().getStorage()
                            .uploadPartiallyToStorage(Triton.get().getStorage().getCollections(), changed, null);
                    sendConfigToServer(server.getServer(), null);
                    Triton.get().getLogger().logDebug("Sign saved!");
                });
            }
        } catch (Exception e1) {
            Triton.get().getLogger().logError(e1, "Failed to read plugin message.");
        }
    }

    public void sendPlayerLanguage(@NonNull VelocityLanguagePlayer lp) {
        lp.getParent().getCurrentServer().ifPresent(server -> sendPlayerLanguage(lp, server.getServer()));
    }

    public void sendPlayerLanguage(@NonNull VelocityLanguagePlayer lp, @NonNull RegisteredServer server) {
        Triton.get().getLogger().logTrace("Sending player %1 language to server %2", lp, server.getServerInfo().getName());
        val out = BridgeSerializer.buildPlayerLanguageData(lp);
        sendPluginMessage(server, out);
    }

    public void sendExecutableCommand(String command, @NonNull RegisteredServer server) {
        Triton.get().getLogger().logTrace("Sending command '%1' to server %2", command, server.getServerInfo().getName());
        val out = BridgeSerializer.buildExecutableCommandData(command);
        sendPluginMessage(server, out);
    }

    public void sendConfigToEveryone() {
        Triton.get().getLogger().logDebug("Sending config and translations to all Spigot servers...");
        try {
            val languageOut = BridgeSerializer.getLanguageDataOutput();

            // Send language files
            for (val info : VelocityTriton.asVelocity().getLoader().getServer().getAllServers()) {
                sendConfigToServer(info, languageOut);
            }
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError(e, "Failed to send config and language items to other servers! Not everything might work " +
                            "as expected");
            e.printStackTrace();
        }
    }

    public void sendConfigToServer(@NonNull RegisteredServer info, byte[] languageOut) {
        Triton.get().getLogger()
                .logDebug("Sending config and translations to '%1' server...", info.getServerInfo().getName());

        if (languageOut == null) languageOut = BridgeSerializer.getLanguageDataOutput();

        for (val message : BridgeSerializer.buildTranslationData(info.getServerInfo().getName(), languageOut)) {
            sendPluginMessage(info, message);
        }

    }

    public void forwardCommand(CommandEvent commandEvent) {
        Triton.get().getLogger().logTrace("Forwarding command '%1' from player '%2'",
                commandEvent.getFullSubCommand(), commandEvent.getSender().getUUID());
        val out = BridgeSerializer.buildForwardCommandData(commandEvent);

        VelocityTriton.asVelocity().getLoader().getServer().getPlayer(commandEvent.getSender().getUUID())
                .flatMap(Player::getCurrentServer)
                .ifPresent(serverConnection -> sendPluginMessage(serverConnection.getServer(), out));
    }

    private void sendPluginMessage(@NonNull RegisteredServer info, byte[] data) {
        if (!info.sendPluginMessage(VelocityTriton.asVelocity().getBridgeChannelIdentifier(), data)) {
            queue.computeIfAbsent(info, server -> new LinkedList<>()).add(data);
        }
    }

    public void executeQueue(@NonNull RegisteredServer server) {
        byte[] data;
        val queue = this.queue.get(server);
        if (queue == null) return;

        while ((data = queue.poll()) != null) {
            server.sendPluginMessage(VelocityTriton.asVelocity().getBridgeChannelIdentifier(), data);
        }
    }
}
