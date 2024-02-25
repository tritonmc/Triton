package com.rexcantor64.triton.bridge;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.VelocityLanguagePlayer;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.NonNull;
import lombok.val;

import java.util.*;

public class VelocityBridgeManager {
    private final Map<RegisteredServer, Queue<byte[]>> queue = new HashMap<>();

    @Subscribe
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getIdentifier().equals(Triton.asVelocity().getBridgeChannelIdentifier())) {
            return;
        }

        // Avoid propagating messages from player to server
        // and ignore message if it doesn't come from a server.
        // Fixes security advisory GHSA-8vj5-jccf-q25r (CVE-2023-30859).
        e.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(e.getSource() instanceof ServerConnection)) {
            return;
        }

        val in = e.dataAsDataStream();

        try {
            val action = in.readByte();

            // Player changes language
            if (action == 0) {
                val uuid = UUID.fromString(in.readUTF());
                val language = in.readUTF();

                val player = (VelocityLanguagePlayer) Triton.get().getPlayerManager().get(uuid);
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
        Triton.get().getLogger().logTrace("Sending player %1 language to server", lp);
        val out = BridgeSerializer.buildPlayerLanguageData(lp);
        sendPluginMessage(lp.getParent(), out);
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
            for (val info : Triton.asVelocity().getLoader().getServer().getAllServers())
                sendConfigToServer(info, languageOut);
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError(e, "Failed to send config and language items to other servers! Not everything might work " +
                            "as expected");
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

        Triton.asVelocity().getLoader().getServer().getPlayer(commandEvent.getSender().getUUID())
                .ifPresent(player -> sendPluginMessage(player, out));
    }

    private void sendPluginMessage(@NonNull RegisteredServer info, byte[] data) {
        if (!info.sendPluginMessage(Triton.asVelocity().getBridgeChannelIdentifier(), data)) {
            queue.computeIfAbsent(info, server -> new LinkedList<>()).add(data);
        }
    }

    private void sendPluginMessage(@NonNull Player player, byte[] data) {
        player.getCurrentServer().ifPresent(serverConnection -> {
            if (!serverConnection.sendPluginMessage(Triton.asVelocity().getBridgeChannelIdentifier(), data)) {
                Triton.get().getLogger().logError("Failed to send plugin message to player %1 (%2)", player.getUsername(), player.getUniqueId());
            }
        });
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
