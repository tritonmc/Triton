package com.rexcantor64.triton.bungeecord.bridge;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.bridge.BridgeSerializer;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.language.item.SignLocation;
import com.rexcantor64.triton.player.LanguagePlayer;
import lombok.NonNull;
import lombok.val;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class BungeeBridgeManager implements Listener, BridgeManager {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals("triton:main") || e.isCancelled()) {
            return;
        }

        // Avoid propagating messages from player to server
        // and ignore message if it doesn't come from a server.
        // Fixes security advisory GHSA-8vj5-jccf-q25r (CVE-2023-30859).
        e.setCancelled(true);
        if (!(e.getSender() instanceof Server)) {
            return;
        }

        val in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            val action = in.readByte();

            // Player changes language
            if (action == BridgeSerializer.ActionS2P.UPDATE_PLAYER_LANGUAGE.getKey()) {
                val uuid = UUID.fromString(in.readUTF());
                val language = in.readUTF();

                val player = (BungeeLanguagePlayer) Triton.get().getPlayerManager().get(uuid);
                if (player != null)
                    Triton.get().runAsync(() -> player
                            .setLang(Triton.get().getLanguageManager().getLanguageByName(language, true), false));
            }

            // Add or remove a location from a sign group using /triton sign
            if (action == BridgeSerializer.ActionS2P.UPDATE_SIGN_GROUP_MEMBERSHIP.getKey()) {
                val server = ((Server) e.getSender()).getInfo();
                SignLocation location = new SignLocation(server.getName(), in.readUTF(), in.readInt(), in.readInt(), in
                        .readInt());

                // Whether we're adding a location to a group or removing one from a group
                boolean add = in.readBoolean();
                val key = add ? in.readUTF() : null;

                val changed = Triton.get().getStorage().toggleLocationForSignGroup(location, key);

                Triton.get().runAsync(() -> {
                    Triton.get().getLogger().logDebug("Saving sign to storage...");
                    Triton.get().getStorage()
                            .uploadPartiallyToStorage(Triton.get().getStorage().getCollections(), changed, null);
                    sendConfigToServer(server, null);
                    Triton.get().getLogger().logDebug("Sign saved!");
                });
            }
        } catch (Exception e1) {
            Triton.get().getLogger().logError(e1, "Failed to read plugin message.");
        }
    }

    public void sendConfigToEveryone() {
        Triton.get().getLogger().logDebug("Sending config and translations to all Spigot servers...");
        try {
            val languageOut = BridgeSerializer.getLanguageDataOutput();

            // Send language files
            for (val info : BungeeTriton.asBungee().getBungeeCord().getServers().values())
                sendConfigToServer(info, languageOut);
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError(e, "Failed to send config and language items to other servers! Not everything might work " +
                            "as expected!");
        }
    }


    public void sendConfigToServer(@NonNull ServerInfo info, byte[] languageOut) {
        Triton.get().getLogger().logDebug("Sending config and translations to '%1' server...", info.getName());

        if (languageOut == null) languageOut = BridgeSerializer.getLanguageDataOutput();

        for (val message : BridgeSerializer.buildTranslationData(info.getName(), languageOut))
            info.sendData("triton:main", message);
    }

    public void sendPlayerLanguage(BungeeLanguagePlayer lp) {
        sendPlayerLanguage(lp, lp.getParent().getServer());
    }

    public void sendPlayerLanguage(@NonNull LanguagePlayer lp, @NonNull Server server) {
        Triton.get().getLogger().logTrace("Sending player %1 language to server %2", lp, server.getInfo().getName());
        val out = BridgeSerializer.buildPlayerLanguageData(lp);
        server.sendData("triton:main", out);
    }

    public void sendExecutableCommand(String command, @NonNull Server server) {
        Triton.get().getLogger().logTrace("Sending command '%1' to server %2", command, server.getInfo().getName());
        val out = BridgeSerializer.buildExecutableCommandData(command);
        server.sendData("triton:main", out);
    }

    public void forwardCommand(CommandEvent commandEvent) {
        Triton.get().getLogger().logTrace("Forwarding command '%1' from player '%2'",
                commandEvent.getFullSubCommand(), commandEvent.getSender().getUUID());
        val out = BridgeSerializer.buildForwardCommandData(commandEvent);

        BungeeTriton.asBungee().getBungeeCord().getPlayer(commandEvent.getSender().getUUID()).getServer()
                .sendData("triton:main", out);
    }

}
