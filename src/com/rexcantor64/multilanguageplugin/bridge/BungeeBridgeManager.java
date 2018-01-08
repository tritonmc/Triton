package com.rexcantor64.multilanguageplugin.bridge;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;

public class BungeeBridgeManager implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals("MultiLanguagePlugin")) return;
        System.out.println("received PME!");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            String channel = in.readUTF();
            if (channel.equals("ping")) {
                System.out.println("Is ping request");
                int id = in.readInt();
                System.out.println("ID: " + id);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(stream);
                try {
                    out.writeUTF("response");
                    out.writeInt(id);
                    out.writeUTF("pong");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                BungeeCord.getInstance().getPlayer(e.getReceiver().toString()).sendData("MultiLanguagePlugin", stream.toByteArray());
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
