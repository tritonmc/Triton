package com.rexcantor64.triton.packetinterceptor;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.components.api.chat.BaseComponent;
import com.rexcantor64.triton.components.chat.ComponentSerializer;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.*;
import org.json.JSONObject;

import java.util.*;

public class BungeeListener implements Connection.Unsafe {

    private BungeeLanguagePlayer owner;

    private HashMap<UUID, String> tabListCache = new HashMap<>();

    public BungeeListener(BungeeLanguagePlayer owner) {
        this.owner = owner;
        owner.setListener(this);
    }

    private void handlePlayerListItem(DefinedPacket packet) {
        PlayerListItem p = (PlayerListItem) packet;
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (PlayerListItem.Item i : p.getItems()) {
            if (p.getAction() == PlayerListItem.Action.UPDATE_DISPLAY_NAME || p.getAction() == PlayerListItem.Action.ADD_PLAYER) {
                if (i.getDisplayName() != null) {
                    try {
                        if (MultiLanguagePlugin.get().getLanguageParser().hasLanguages(i.getDisplayName(), MultiLanguagePlugin.get().getConf().getTabSyntax())) {
                            PlayerListItem.Item item = clonePlayerListItem(i);
                            tabListCache.put(item.getUuid(), item.getDisplayName());
                            JSONObject obj = new JSONObject(item.getDisplayName());
                            obj.put("text", MultiLanguagePlugin.get().getLanguageParser().replaceLanguages(obj.getString("text"), owner, MultiLanguagePlugin.get().getConf().getTabSyntax()));
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

    private void handleChat(DefinedPacket packet) {
        Chat p = (Chat) packet;
        int type = p.getPosition();
        if ((type == 2 && !MultiLanguagePlugin.get().getConf().isActionbars()) || (type != 2 && !MultiLanguagePlugin.get().getConf().isChat()))
            return;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        if (type != 2) {
            text = MultiLanguagePlugin.get().getLanguageParser().parseChat(owner, MultiLanguagePlugin.get().getConf().getChatSyntax(), text);
        } else {
            text = MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, text, MultiLanguagePlugin.get().getConf().getActionbarSyntax());
        }
        p.setMessage(ComponentSerializer.toString(text));
    }

    private void handleTitle(DefinedPacket packet) {
        Title p = (Title) packet;
        p.setText(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseTitle(owner, ComponentSerializer.parse(p.getText()), MultiLanguagePlugin.get().getConf().getTitleSyntax())));
    }

    private void handleBossbar(DefinedPacket packet) {
        BossBar p = (BossBar) packet;
        UUID uuid = p.getUuid();
        if (p.getAction() == 1) {
            owner.removeBossbar(uuid);
            return;
        }
        if (p.getAction() != 0 && p.getAction() != 3) return;
        owner.setBossbar(uuid, p.getTitle());
        p.setTitle(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseTitle(owner, ComponentSerializer.parse(p.getTitle()), MultiLanguagePlugin.get().getConf().getBossbarSyntax())));
    }

    private void handlePlayerListHeaderFooter(DefinedPacket packet) {
        PlayerListHeaderFooter p = (PlayerListHeaderFooter) packet;
        owner.setLastTabHeader(p.getHeader());
        owner.setLastTabFooter(p.getFooter());
        p.setHeader(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, ComponentSerializer.parse(p.getHeader()), MultiLanguagePlugin.get().getConf().getTabSyntax())));
        p.setFooter(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, ComponentSerializer.parse(p.getFooter()), MultiLanguagePlugin.get().getConf().getTabSyntax())));
    }

    private void handleKick(DefinedPacket packet) {
        Kick p = (Kick) packet;
        System.out.println(p);
        p.setMessage(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, ComponentSerializer.parse(p.getMessage()), MultiLanguagePlugin.get().getConf().getKickSyntax())));
    }

    @Override
    public void sendPacket(DefinedPacket packet) {
        if (packet instanceof PlayerListItem && MultiLanguagePlugin.get().getConf().isTab())
            handlePlayerListItem(packet);
        else if (packet instanceof Chat)
            handleChat(packet);
        else if (packet instanceof Title && MultiLanguagePlugin.get().getConf().isTitles())
            handleTitle(packet);
        else if (packet instanceof BossBar && MultiLanguagePlugin.get().getConf().isBossbars())
            handleBossbar(packet);
        else if (packet instanceof PlayerListHeaderFooter && MultiLanguagePlugin.get().getConf().isTab())
            handlePlayerListHeaderFooter(packet);
        else if (packet instanceof Kick && MultiLanguagePlugin.get().getConf().isKick())
            handleKick(packet);

        send(packet);
    }

    private void send(DefinedPacket packet) {
        ((ChannelWrapper) NMSUtils.getDeclaredField(owner.getCurrentConnection(), "ch")).write(packet);
    }

    public void refreshTab() {
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (Map.Entry<UUID, String> item : tabListCache.entrySet()) {
            PlayerListItem.Item i = new PlayerListItem.Item();
            i.setUuid(item.getKey());
            i.setDisplayName(MultiLanguagePlugin.get().getLanguageParser().replaceLanguages(item.getValue(), owner, MultiLanguagePlugin.get().getConf().getTabSyntax()));
            items.add(i);
        }
        PlayerListItem packet = new PlayerListItem();
        packet.setAction(PlayerListItem.Action.UPDATE_DISPLAY_NAME);
        packet.setItems(items.toArray(new PlayerListItem.Item[0]));
        send(packet);
    }

    public void refreshBossbar(UUID uuid, String json) {
        if (owner.getParent().getPendingConnection().getVersion() < 107) return;
        BossBar p = new BossBar(uuid, 3);
        p.setTitle(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseTitle(owner, ComponentSerializer.parse(json), MultiLanguagePlugin.get().getConf().getBossbarSyntax())));
        send(p);
    }

    public void refreshTabHeaderFooter(String header, String footer) {
        PlayerListHeaderFooter p = new PlayerListHeaderFooter();
        p.setHeader(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, ComponentSerializer.parse(header), MultiLanguagePlugin.get().getConf().getTabSyntax())));
        p.setFooter(ComponentSerializer.toString(MultiLanguagePlugin.get().getLanguageParser().parseSimpleBaseComponent(owner, ComponentSerializer.parse(footer), MultiLanguagePlugin.get().getConf().getTabSyntax())));
        send(p);
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
