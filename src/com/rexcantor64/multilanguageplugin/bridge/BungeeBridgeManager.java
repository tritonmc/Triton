package com.rexcantor64.multilanguageplugin.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.player.BungeeLanguagePlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.UUID;

public class BungeeBridgeManager implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals("MultiLanguagePlugin")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            byte action = in.readByte();
            if (action == 0)
                MultiLanguagePlugin.asBungee().getLanguagePlayer(UUID.fromString(in.readUTF())).setLanguage(MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(in.readUTF(), true));
        } catch (Exception e1) {
            MultiLanguagePlugin.get().logError("Failed to read plugin message: %1", e1.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        BungeeLanguagePlayer lp = MultiLanguagePlugin.asBungee().registerPlayer(event.getPlayer());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(lp.getLanguage().getName());
        event.getPlayer().sendData("MultiLanguagePlugin", out.toByteArray());
    }

}
