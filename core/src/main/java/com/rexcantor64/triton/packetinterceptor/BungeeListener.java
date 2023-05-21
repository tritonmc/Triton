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
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.*;

import java.util.*;

public class BungeeListener extends MessageToMessageEncoder<DefinedPacket> {

    private final BungeeLanguagePlayer owner;
    private final int protocolVersion;

    private final HashMap<UUID, String> tabListCache = new HashMap<>();

    public BungeeListener(BungeeLanguagePlayer owner, int protocolVersion) {
        this.owner = owner;
        this.protocolVersion = protocolVersion;
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
                                Triton.get().getConf().getTabSyntax());
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

    private void handlePlayerListItemUpdate(DefinedPacket packet) {
        PlayerListItemUpdate p = (PlayerListItemUpdate) packet;
        if (!p.getActions().contains(PlayerListItemUpdate.Action.UPDATE_DISPLAY_NAME)) {
            return;
        }
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (PlayerListItem.Item i : p.getItems()) {
            if (i.getDisplayName() != null) {
                try {
                    String original = TextComponent.toLegacyText(ComponentSerializer.parse(i.getDisplayName()));
                    String translated = translate(original,
                            Triton.get().getConf().getTabSyntax());
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
            items.add(i);
        }
        p.setItems(items.toArray(new PlayerListItem.Item[0]));
    }

    private void handlePlayerListItemRemove(DefinedPacket packet) {
        PlayerListItemRemove p = (PlayerListItemRemove) packet;
        for (UUID uuid : p.getUuids()) {
            tabListCache.remove(uuid);
        }
    }

    private boolean handleChat(DefinedPacket packet) {
        Chat p = (Chat) packet;
        int type = p.getPosition();
        if ((type == 2 && !Triton.get().getConf().isActionbars()) || (type != 2 && !Triton.get().getConf().isChat()))
            return true;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        text = Triton.get().getLanguageParser().parseComponent(owner, type != 2 ?
                Triton.get().getConf().getChatSyntax() : Triton.get().getConf().getActionbarSyntax(), text);
        if (text == null)
            return false;

        if (type == 2 && protocolVersion <= ProtocolConstants.MINECRAFT_1_10) {
            // The Notchian client does not support true JSON messages on actionbars
            // on 1.10 and below. Therefore, we must convert to a legacy string inside
            // a TextComponent.
            p.setMessage(ComponentSerializer.toString(new TextComponent(TextComponent.toLegacyText(text))));
        } else {
            p.setMessage(ComponentSerializer.toString(text));
        }
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
        if ((type == 2 && !Triton.get().getConf().isActionbars()) || (type != 2 && !Triton.get().getConf().isChat()))
            return true;
        BaseComponent[] text = ComponentSerializer.parse(p.getMessage());
        text = Triton.get().getLanguageParser().parseComponent(owner, type != 2 ?
                Triton.get().getConf().getChatSyntax() : Triton.get().getConf().getActionbarSyntax(), text);
        if (text == null)
            return false;
        p.setMessage(ComponentSerializer.toString(text));
        return true;
    }

    private boolean handleTitle(DefinedPacket packet) {
        Title p = (Title) packet;
        if (p.getText() == null) return true;
        BaseComponent[] result = Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTitleSyntax(), ComponentSerializer.parse(p.getText()));
        if (result == null)
            return false;
        p.setText(ComponentSerializer.toString(result));
        return true;
    }

    private boolean handleSubtitle(DefinedPacket packet) {
        Subtitle p = (Subtitle) packet;
        if (p.getText() == null) return true;
        BaseComponent[] result = Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTitleSyntax(), ComponentSerializer.parse(p.getText()));
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
                Triton.get().getConf().getBossbarSyntax(), ComponentSerializer.parse(p.getTitle()))));
    }

    private void handlePlayerListHeaderFooter(DefinedPacket packet) {
        PlayerListHeaderFooter p = (PlayerListHeaderFooter) packet;
        owner.setLastTabHeader(p.getHeader());
        owner.setLastTabFooter(p.getFooter());
        p.setHeader(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(p.getHeader()))));
        p.setFooter(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getTabSyntax(), ComponentSerializer.parse(p.getFooter()))));
    }

    private void handleKick(DefinedPacket packet) {
        Kick p = (Kick) packet;
        p.setMessage(serializeComponent(Triton.get().getLanguageParser().parseComponent(owner,
                Triton.get().getConf().getKickSyntax(), ComponentSerializer.parse(p.getMessage()))));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket packet,
                          List<Object> out) {
        try {
            if (Triton.get().getConf().isTab() && packet instanceof PlayerListItem) {
                handlePlayerListItem(packet);
            } else if (Triton.get().getConf().isTab() && packet instanceof PlayerListItemUpdate) {
                handlePlayerListItemUpdate(packet);
            } else if (Triton.get().getConf().isTab() && packet instanceof PlayerListItemRemove) {
                handlePlayerListItemRemove(packet);
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
            } else if (Triton.get().getConf().isTitles() && packet instanceof Title) {
                if (!handleTitle(packet)) {
                    out.add(false);
                    return;
                }
            } else if (Triton.get().getConf().isTitles() && packet instanceof Subtitle) {
                if (!handleSubtitle(packet)) {
                    out.add(false);
                    return;
                }
            } else if (Triton.get().getConf().isBossbars() && packet instanceof BossBar) {
                handleBossbar(packet);
            } else if (Triton.get().getConf().isTab() && packet instanceof PlayerListHeaderFooter) {
                handlePlayerListHeaderFooter(packet);
            } else if (Triton.get().getConf().isKick() && packet instanceof Kick) {
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
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19_3) {
            PlayerListItemUpdate packet = new PlayerListItemUpdate();
            packet.setActions(EnumSet.of(PlayerListItemUpdate.Action.UPDATE_DISPLAY_NAME));
            packet.setItems(items.toArray(new PlayerListItem.Item[0]));
            send(packet);
        } else {
            PlayerListItem packet = new PlayerListItem();
            packet.setAction(PlayerListItem.Action.UPDATE_DISPLAY_NAME);
            packet.setItems(items.toArray(new PlayerListItem.Item[0]));
            send(packet);
        }
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

        item1.setUsername(item.getUsername());
        item1.setProperties(item.getProperties());

        item1.setChatSessionId(item.getChatSessionId());
        item1.setPublicKey(item.getPublicKey());

        item1.setListed(item.getListed());

        item1.setGamemode(item.getGamemode());

        item1.setPing(item.getPing());

        item1.setDisplayName(item.getDisplayName());
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
