package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.packetinterceptor.BungeeDecoder;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.LocationUtils;
import com.rexcantor64.triton.utils.NMSUtils;
import io.netty.channel.Channel;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.netty.PipelineUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BungeeBridgeManager implements Listener {

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (!e.getTag().equals("triton:main")) return;
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

        try {
            byte action = in.readByte();
            if (action == 0)
                Triton.get().getPlayerManager().get(UUID.fromString(in.readUTF())).setLang(Triton.get().getLanguageManager().getLanguageByName(in.readUTF(), true));
            else if (action == 1) {
                String server = ((Server) e.getSender()).getInfo().getName();
                LanguageSign.SignLocation loc = new LanguageSign.SignLocation(server, in.readUTF(), in.readInt(), in.readInt(), in.readInt());
                boolean add = in.readBoolean();
                String key = "";
                if (add)
                    key = in.readUTF();
                List<String> remove = new ArrayList<>();
                for (LanguageItem li : Triton.get().getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN))
                    if (((LanguageSign) li).hasLocation(loc, true)) remove.add(li.getKey());
                JSONArray raw = Triton.get().getLanguageConfig().getRaw();
                for (int i = 0; i < raw.length(); i++) {
                    JSONObject obj = raw.optJSONObject(i);
                    if (obj == null || !obj.optString("type", "text").equals("sign")) continue;
                    if (!remove.isEmpty() && remove.contains(obj.optString("key"))) {
                        JSONArray locs = obj.optJSONArray("locations");
                        if (locs == null) continue;
                        for (int k = 0; k < locs.length(); k++) {
                            JSONObject l = locs.optJSONObject(k);
                            if (l != null && l.optString("server").equals(server) && l.optString("world").equals(loc.getWorld()) && l.optInt("x") == loc.getX() && l.optInt("y") == loc.getY() && l.optInt("z") == loc.getZ())
                                locs.remove(k--);
                        }
                        obj.put("locations", locs);
                        raw.put(i, obj);
                    }
                    if (add && obj.optString("key").equals(key)) {
                        JSONArray locs = obj.optJSONArray("locations");
                        if (locs == null) locs = new JSONArray();
                        locs.put(LocationUtils.locationToJSON(loc.getX(), loc.getY(), loc.getZ(), loc.getWorld()).put("server", server));
                        obj.put("locations", locs);
                        raw.put(i, obj);
                    }
                }
                Triton.get().getLanguageConfig().saveFromRaw(raw);
                Triton.get().reload();
            }
        } catch (Exception e1) {
            Triton.get().logError("Failed to read plugin message: %1", e1.getMessage());
            if (Triton.get().getConf().isDebug())
                e1.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        BungeeLanguagePlayer lp = (BungeeLanguagePlayer) Triton.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(event.getPlayer().getUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        event.getServer().sendData("triton:main", out.toByteArray());
        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            lp.executeCommands(event.getServer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        if (!Triton.get().getConf().isKick()) return;
        event.registerIntent(Triton.get().getLoader().asBungee());
        BungeeLanguagePlayer lp = Triton.get().getPlayerManager().registerBungee(event.getConnection().getUniqueId(), new BungeeLanguagePlayer(event.getConnection().getUniqueId(), event.getConnection()));
        BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), new Runnable() {
            @Override
            public void run() {
                if (event.getCancelReasonComponents() != null)
                    event.setCancelReason(Triton.get().getLanguageParser().parseComponent(lp, Triton.get().getConf().getKickSyntax(), event.getCancelReasonComponents()));
                event.completeIntent(Triton.get().getLoader().asBungee());
            }
        });
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        BungeeLanguagePlayer lp = (BungeeLanguagePlayer) Triton.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        Triton.asBungee().setCustomUnsafe(lp);
        try {
            ProxiedPlayer p = event.getPlayer();
            Object ch =
                    NMSUtils.getDeclaredField(p, "ch");
            Method method = ch.getClass().getDeclaredMethod("getHandle");
            Channel channel = (Channel) method.invoke(ch, new Object[0]);
            channel.pipeline().addAfter(PipelineUtils.PACKET_DECODER, "triton-custom-decoder", new BungeeDecoder(lp));
        } catch (Exception e) {
            System.out.println("[BungeePackets] Failed to inject client connection for " + event.getPlayer().getName());
        }
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        Triton.get().getPlayerManager().unregisterPlayer(event.getPlayer().getUniqueId());
        //MultiLanguagePlugin.asBungee().setDefaultUnsafe(event.getPlayer());
    }


}
