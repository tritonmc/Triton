package com.rexcantor64.multilanguageplugin.packetinterceptor;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.player.BungeeLanguagePlayer;
import com.rexcantor64.multilanguageplugin.utils.NMSUtils;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import org.json.JSONObject;

import java.util.*;

public class BungeeListener implements Connection.Unsafe {

    private BungeeLanguagePlayer owner;

    private HashMap<UUID, String> tabListCache = new HashMap<>();

    public BungeeListener(BungeeLanguagePlayer owner) {
        this.owner = owner;
        owner.setListener(this);
    }

    @Override
    public void sendPacket(DefinedPacket packet) {
        if (packet instanceof PlayerListItem && MultiLanguagePlugin.get().getConf().isTab()) {
            PlayerListItem p = (PlayerListItem) packet;
            List<PlayerListItem.Item> items = new ArrayList<>();
            for (PlayerListItem.Item i : p.getItems()) {
                if (p.getAction() == PlayerListItem.Action.UPDATE_DISPLAY_NAME || p.getAction() == PlayerListItem.Action.ADD_PLAYER) {
                    if (i.getDisplayName() != null) {
                        try {
                            if (MultiLanguagePlugin.get().getLanguageParser().hasLanguages(i.getDisplayName())) {
                                PlayerListItem.Item item = clonePlayerListItem(i);
                                tabListCache.put(item.getUuid(), item.getDisplayName());
                                JSONObject obj = new JSONObject(item.getDisplayName());
                                obj.put("text", MultiLanguagePlugin.get().getLanguageParser().replaceLanguages(obj.getString("text"), owner));
                                item.setDisplayName(obj.toString());
                                items.add(item);
                                continue;
                            } else tabListCache.remove(i.getUuid());
                        } catch (Exception e) {
                            if (MultiLanguagePlugin.get().getConf().isDebug())
                                e.printStackTrace();
                        }
                    } else
                        tabListCache.remove(i.getUuid());
                } else if (p.getAction() == PlayerListItem.Action.REMOVE_PLAYER)
                    tabListCache.remove(i.getUuid());
                items.add(i);
            }
            p.setItems(items.toArray(new PlayerListItem.Item[0]));
        }
        send(packet);
    }

    private void send(DefinedPacket packet) {
        ((ChannelWrapper) NMSUtils.getDeclaredField(owner.getParent(), "ch")).write(packet);
    }

    public void refreshTab() {
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (Map.Entry<UUID, String> item : tabListCache.entrySet()) {
            PlayerListItem.Item i = new PlayerListItem.Item();
            i.setUuid(item.getKey());
            i.setDisplayName(MultiLanguagePlugin.get().getLanguageParser().replaceLanguages(item.getValue(), owner));
            items.add(i);
        }
        PlayerListItem packet = new PlayerListItem();
        packet.setAction(PlayerListItem.Action.UPDATE_DISPLAY_NAME);
        packet.setItems(items.toArray(new PlayerListItem.Item[0]));
        send(packet);
    }

    private PlayerListItem.Item clonePlayerListItem(PlayerListItem.Item item) {
        PlayerListItem.Item item1 = new PlayerListItem.Item();
        item1.setUuid(item.getUuid());
        item1.setDisplayName(item.getDisplayName());
        item1.setGamemode(item.getGamemode());
        item1.setProperties(item.getProperties());
        item1.setPing(item.getPing());
        item1.setUsername(item.getUsername());
        return item1;
    }

}
