package com.rexcantor64.multilanguageplugin.bridge;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.HashMap;

public class SpigotBridgeManager implements PluginMessageListener {

    private HashMap<Integer, Object> responses = new HashMap<>();
    private int index = 0;

    private static final int MAX_INDEX = 10000;

    @Override
    public synchronized void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!channel.equals("MultiLanguagePlugin")) return;

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        try {
            String subchannel = in.readUTF();
            if (subchannel.equals("response")) {
                int id = in.readInt();
                String input = in.readUTF();
                responses.put(id, input);

                notifyAll();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized int getPing(Player p) {
        int id = getNextId();
        sendPingRequest(p, id);

        long start = System.currentTimeMillis();

        try {
            wait(10000);
        } catch (InterruptedException e) {
        }


        long end = System.currentTimeMillis();

        if (!responses.get(id).equals("pong")) return -1;

        return (int) (end - start);
    }

    private boolean sendPingRequest(Player p, int id) {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("ping");
            out.writeInt(id);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        p.sendPluginMessage(MultiLanguagePlugin.get().getLoader().asSpigot(), "MultiLanguagePlugin", b.toByteArray());
        return true;
    }

    private int getNextId() {
        if (index + 1 > MAX_INDEX) index = 0;
        return index++;
    }
}
