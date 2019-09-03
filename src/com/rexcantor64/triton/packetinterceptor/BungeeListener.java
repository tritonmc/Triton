package com.rexcantor64.triton.packetinterceptor;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.chat.ComponentSerializer;
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
                        String original = new JSONObject(i.getDisplayName()).getString("text");
                        String translated = translate(original,
                                Triton.get().getConf().getTabSyntax());
                        if (!original.equals(translated)) {
                            PlayerListItem.Item item = clonePlayerListItem(i);
                            tabListCache.put(item.getUuid(), item.getDisplayName());
                            JSONObject obj = new JSONObject(item.getDisplayName());
                            obj.put("text", translated);
                            item.setDisplayName(obj.toString());
                            items.add(item);
                            continue;
                        } else tabListCache.remove(i.getUuid());
                    } catch (Exception e) {
                        if (Triton.get().getConf().isDebug())
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
        if ((type == 2 && !Triton.get().getConf().isActionbars()) || (type != 2 && !Triton.get().getConf().isChat()))
            return;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        text = Triton.get().getLanguageParser().parseComponent(owner, type != 2 ?
                Triton.get().getConf().getChatSyntax() : Triton.get().getConf().getActionbarSyntax(), text);
        p.setMessage(ComponentSerializer.toString(text));
    }

    private void handleTitle(DefinedPacket packet) {
        Title p = (Title) packet;
        if (p.getText() != null)
            p.setText(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                    Triton.get().getConf().getTitleSyntax(), ComponentSerializer.parse(p.getText()))));
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
        p.setTitle(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getBossbarSyntax(), ComponentSerializer.parse(p.getTitle()))));
    }

    private void handlePlayerListHeaderFooter(DefinedPacket packet) {
        PlayerListHeaderFooter p = (PlayerListHeaderFooter) packet;
        owner.setLastTabHeader(p.getHeader());
        owner.setLastTabFooter(p.getFooter());
        p.setHeader(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(p.getHeader()))));
        p.setFooter(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(p.getFooter()))));
    }

    private void handleKick(DefinedPacket packet) {
        Kick p = (Kick) packet;
        p.setMessage(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getKickSyntax(), ComponentSerializer.parse(p.getMessage()))));
    }

    @Override
    public void sendPacket(DefinedPacket packet) {
        if (packet instanceof PlayerListItem && Triton.get().getConf().isTab())
            handlePlayerListItem(packet);
        else if (packet instanceof Chat)
            handleChat(packet);
        else if (packet instanceof Title && Triton.get().getConf().isTitles())
            handleTitle(packet);
        else if (packet instanceof BossBar && Triton.get().getConf().isBossbars())
            handleBossbar(packet);
        else if (packet instanceof PlayerListHeaderFooter && Triton.get().getConf().isTab())
            handlePlayerListHeaderFooter(packet);
        else if (packet instanceof Kick && Triton.get().getConf().isKick())
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
            i.setDisplayName(translate(item.getValue(),
                    Triton.get().getConf().getTabSyntax()));
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
        p.setTitle(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getBossbarSyntax(), ComponentSerializer.parse(json))));
        send(p);
    }

    public void refreshTabHeaderFooter(String header, String footer) {
        PlayerListHeaderFooter p = new PlayerListHeaderFooter();
        p.setHeader(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(header))));
        p.setFooter(ComponentSerializer.toString(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(footer))));
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

    private String translate(String s, MainConfig.FeatureSyntax syntax) {
        return Triton.get().getLanguageParser().replaceLanguages(Triton.get().getLanguageManager().getMatch(s, owner)
                , owner, syntax);
    }

}
