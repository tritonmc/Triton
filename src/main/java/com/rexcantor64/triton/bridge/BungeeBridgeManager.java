package com.rexcantor64.triton.bridge;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.LocationUtils;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.*;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
                Triton.get().getPlayerManager().get(UUID.fromString(in.readUTF()))
                        .setLang(Triton.get().getLanguageManager().getLanguageByName(in.readUTF(), true));
            else if (action == 1) {
                String server = ((Server) e.getSender()).getInfo().getName();
                LanguageSign.SignLocation loc = new LanguageSign.SignLocation(server, in.readUTF(), in.readInt(), in
                        .readInt(), in.readInt());
                boolean add = in.readBoolean();
                String key = "";
                if (add)
                    key = in.readUTF();
                List<String> remove = new ArrayList<>();
                for (LanguageItem li : Triton.get().getLanguageManager()
                        .getAllItems(LanguageItem.LanguageItemType.SIGN))
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
                            if (l != null && l.optString("server").equals(server) && l.optString("world")
                                    .equals(loc.getWorld()) && l.optInt("x") == loc.getX() && l.optInt("y") == loc
                                    .getY() && l.optInt("z") == loc.getZ())
                                locs.remove(k--);
                        }
                        obj.put("locations", locs);
                        raw.put(i, obj);
                    }
                    if (add && obj.optString("key").equals(key)) {
                        JSONArray locs = obj.optJSONArray("locations");
                        if (locs == null) locs = new JSONArray();
                        locs.put(LocationUtils.locationToJSON(loc.getX(), loc.getY(), loc.getZ(), loc.getWorld())
                                .put("server", server));
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
        BungeeLanguagePlayer lp = (BungeeLanguagePlayer) Triton.get().getPlayerManager()
                .get(event.getPlayer().getUniqueId());
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(event.getPlayer().getUniqueId().toString());
        out.writeUTF(lp.getLang().getName());
        event.getServer().sendData("triton:main", out.toByteArray());
        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            lp.executeCommands(event.getServer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        Plugin plugin = Triton.get().getLoader().asBungee();
        event.registerIntent(plugin);
        BungeeCord.getInstance().getScheduler().runAsync(plugin, () -> {
            BungeeLanguagePlayer lp = Triton.get().getPlayerManager()
                    .registerBungee(event.getConnection().getUniqueId(), new BungeeLanguagePlayer(event.getConnection()
                            .getUniqueId(), event.getConnection()));
            BungeeMLP.asBungee().injectPipeline(lp, event.getConnection());
            event.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        Triton.get().getPlayerManager().unregisterPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMotd(ProxyPingEvent event) {
        Plugin plugin = Triton.get().getLoader().asBungee();
        event.registerIntent(plugin);
        BungeeCord.getInstance().getScheduler().runAsync(plugin, () -> {
            if (Triton.get().getConf().isMotd())
                event.getResponse()
                        .setDescriptionComponent(componentArrayToSingle(Triton.get().getLanguageParser()
                                .parseComponent(Triton.get().getPlayerStorage()
                                        .getLanguageFromIp(event.getConnection().getAddress().getAddress()
                                                .getHostAddress())
                                        .getName(), Triton.get().getConf()
                                        .getMotdSyntax(), event.getResponse().getDescriptionComponent())));
            event.completeIntent(plugin);
        });

    }

    private BaseComponent componentArrayToSingle(BaseComponent... c) {
        if (c.length == 1) return c[0];
        BaseComponent result = new TextComponent("");
        result.setExtra(Arrays.asList(c));
        return result;
    }
}
