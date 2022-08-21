package com.rexcantor64.triton.packetinterceptor;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.NMSUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.packet.*;

import java.util.*;

public class BungeeListener extends MessageToMessageEncoder<DefinedPacket> {

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
            if (p.getAction() == PlayerListItem.Action.UPDATE_DISPLAY_NAME || p
                    .getAction() == PlayerListItem.Action.ADD_PLAYER) {
                if (i.getDisplayName() != null) {
                    try {
                        String original = TextComponent.toLegacyText(ComponentSerializer.parse(i.getDisplayName()));
                        String translated = translate(original,
                                Triton.get().getConfig().getTabSyntax());
                        if (!original.equals(translated)) {
                            PlayerListItem.Item item = clonePlayerListItem(i);
                            tabListCache.put(item.getUuid(), item.getDisplayName());
                            item.setDisplayName(ComponentSerializer.toString(TextComponent.fromLegacyText(translated)));
                            items.add(item);
                            continue;
                        } else tabListCache.remove(i.getUuid());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    tabListCache.remove(i.getUuid());
                }
            } else if (p.getAction() == PlayerListItem.Action.REMOVE_PLAYER)
                tabListCache.remove(i.getUuid());
            items.add(i);
        }
        p.setItems(items.toArray(new PlayerListItem.Item[0]));
    }

    private boolean handleChat(DefinedPacket packet) {
        Chat p = (Chat) packet;
        int type = p.getPosition();
        if ((type == 2 && !Triton.get().getConfig().isActionbars()) || (type != 2 && !Triton.get().getConfig().isChat()))
            return true;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        text = Triton.get().getLanguageParser().parseComponent(owner, type != 2 ?
                Triton.get().getConfig().getChatSyntax() : Triton.get().getConfig().getActionbarSyntax(), text);
        if (text == null)
            return false;
        p.setMessage(ComponentSerializer.toString(text));
        return true;
    }

    /**
     * Handles a system chat packet, added in Minecraft 1.19.
     *
     * @param packet A SystemChat packet.
     * @return True if the packet should be sent, false if it should be cancelled.
     */
    private boolean handleSystemChat(DefinedPacket packet) {
        SystemChat p = (SystemChat) packet;
        int type = p.getPosition();
        if ((type == 2 && !Triton.get().getConfig().isActionbars()) || (type != 2 && !Triton.get().getConfig().isChat()))
            return true;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        text = Triton.get().getLanguageParser().parseComponent(owner, type != 2 ?
                Triton.get().getConfig().getChatSyntax() : Triton.get().getConfig().getActionbarSyntax(), text);
        if (text == null)
            return false;
        p.setMessage(ComponentSerializer.toString(text));
        return true;
    }

    private boolean handleTitle(DefinedPacket packet) {
        Title p = (Title) packet;
        if (p.getText() == null) return true;
        BaseComponent[] result = Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getTitleSyntax(), ComponentSerializer.parse(p.getText()));
        if (result == null)
            return false;
        p.setText(ComponentSerializer.toString(result));
        return true;
    }

    private boolean handleSubtitle(DefinedPacket packet) {
        Subtitle p = (Subtitle) packet;
        if (p.getText() == null) return true;
        BaseComponent[] result = Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getTitleSyntax(), ComponentSerializer.parse(p.getText()));
        if (result == null)
            return false;
        p.setText(ComponentSerializer.toString(result));
        return true;
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
        p.setTitle(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getBossbarSyntax(), ComponentSerializer.parse(p.getTitle()))));
    }

    private void handlePlayerListHeaderFooter(DefinedPacket packet) {
        PlayerListHeaderFooter p = (PlayerListHeaderFooter) packet;
        owner.setLastTabHeader(p.getHeader());
        owner.setLastTabFooter(p.getFooter());
        p.setHeader(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getTabSyntax(), ComponentSerializer.parse(p.getHeader()))));
        p.setFooter(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getTabSyntax(), ComponentSerializer.parse(p.getFooter()))));
    }

    private void handleKick(DefinedPacket packet) {
        Kick p = (Kick) packet;
        p.setMessage(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConfig().getKickSyntax(), ComponentSerializer.parse(p.getMessage()))));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket packet,
                          List<Object> out) {
        try {
            if (Triton.get().getConfig().isTab() && packet instanceof PlayerListItem) {
                handlePlayerListItem(packet);
            } else if (packet instanceof Chat) {
                if (!handleChat(packet)) {
                    out.add(false);
                    return;
                }
            } else if (packet instanceof SystemChat) {
                if (!handleSystemChat(packet)) {
                    out.add(false);
                    return;
                }
            } else if (Triton.get().getConfig().isTitles() && packet instanceof Title) {
                if (!handleTitle(packet)) {
                    out.add(false);
                    return;
                }
            } else if (Triton.get().getConfig().isTitles() && packet instanceof Subtitle) {
                if (!handleSubtitle(packet)) {
                    out.add(false);
                    return;
                }
            } else if (Triton.get().getConfig().isBossbars() && packet instanceof BossBar) {
                handleBossbar(packet);
            } else if (Triton.get().getConfig().isTab() && packet instanceof PlayerListHeaderFooter) {
                handlePlayerListHeaderFooter(packet);
            } else if (Triton.get().getConfig().isKick() && packet instanceof Kick) {
                handleKick(packet);
            }
        } catch (Exception | Error e) {
            e.printStackTrace();
        }
        out.add(packet);
    }

    private void send(DefinedPacket packet) {
        ((ChannelWrapper) NMSUtils.getDeclaredField(owner.getCurrentConnection(), "ch")).write(packet);
    }

    public void refreshTab() {
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (Map.Entry<UUID, String> item : tabListCache.entrySet()) {
            PlayerListItem.Item i = new PlayerListItem.Item();
            i.setUuid(item.getKey());
            i.setDisplayName(item.getValue());
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
        p.setTitle(json);
        send(p);
    }

    public void refreshTabHeaderFooter(String header, String footer) {
        send(new PlayerListHeaderFooter(header, footer));
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
        String result = Triton.get().getLanguageParser()
                .replaceLanguages(Triton.get().getLanguageManager().matchPattern(s, owner), owner, syntax);
        if (result == null) result = "";
        return result;
    }

    private String serializeComponent(BaseComponent... bc) {
        if (bc == null) return ComponentSerializer.toString(new TranslatableComponent(""));
        return ComponentSerializer.toString(bc);
    }

}
