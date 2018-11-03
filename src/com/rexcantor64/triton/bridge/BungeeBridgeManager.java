package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.player.LanguagePlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
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
        if (!e.getTag().equals("triton:main")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            byte action = in.readByte();
            if (action == 0)
                MultiLanguagePlugin.get().getPlayerManager().get(UUID.fromString(in.readUTF())).setLang(MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(in.readUTF(), true));
        } catch (Exception e1) {
            MultiLanguagePlugin.get().logError("Failed to read plugin message: %1", e1.getMessage());
            if (MultiLanguagePlugin.get().getConf().isDebug())
                e1.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        LanguagePlayer lp = MultiLanguagePlugin.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(event.getPlayer().getUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        event.getServer().sendData("triton:main", out.toByteArray());
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        BungeeLanguagePlayer lp = (BungeeLanguagePlayer) MultiLanguagePlugin.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        MultiLanguagePlugin.asBungee().setCustomUnsafe(lp);
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        MultiLanguagePlugin.get().getPlayerManager().unregisterPlayer(event.getPlayer().getUniqueId());
        //MultiLanguagePlugin.asBungee().setDefaultUnsafe(event.getPlayer());
    }


}
