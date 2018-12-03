package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.utils.LocationUtils;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
                MultiLanguagePlugin.get().getPlayerManager().get(UUID.fromString(in.readUTF())).setLang(MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(in.readUTF(), true));
            else if (action == 1) {
                String server = ((Server) e.getSender()).getInfo().getName();
                LanguageSign.SignLocation loc = new LanguageSign.SignLocation(server, in.readUTF(), in.readInt(), in.readInt(), in.readInt());
                boolean add = in.readBoolean();
                String key = "";
                if (add)
                    key = in.readUTF();
                List<String> remove = new ArrayList<>();
                for (LanguageItem li : MultiLanguagePlugin.get().getLanguageManager().getAllItems(LanguageItem.LanguageItemType.SIGN))
                    if (((LanguageSign) li).hasLocation(loc, true)) remove.add(li.getKey());
                JSONArray raw = MultiLanguagePlugin.get().getLanguageConfig().getRaw();
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
                MultiLanguagePlugin.get().getLanguageConfig().saveFromRaw(raw);
                MultiLanguagePlugin.get().reload();
            }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(LoginEvent event) {
        if (!MultiLanguagePlugin.get().getConf().isKick()) return;
        event.registerIntent(MultiLanguagePlugin.get().getLoader().asBungee());
        BungeeLanguagePlayer lp = MultiLanguagePlugin.get().getPlayerManager().registerBungee(event.getConnection().getUniqueId(), new BungeeLanguagePlayer(event.getConnection().getUniqueId(), event.getConnection()));
        BungeeCord.getInstance().getScheduler().runAsync(MultiLanguagePlugin.get().getLoader().asBungee(), new Runnable() {
            @Override
            public void run() {
                if (event.getCancelReasonComponents() != null)
                    event.setCancelReason(ComponentSerializer.parse(com.rexcantor64.triton.components.chat.ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseChat(lp, MultiLanguagePlugin.get().getConf().getKickSyntax(), com.rexcantor64.triton.components.chat.ComponentSerializer.parse(ComponentSerializer.toString(event.getCancelReasonComponents()))))));
                event.completeIntent(MultiLanguagePlugin.get().getLoader().asBungee());
            }
        });
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
