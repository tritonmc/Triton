package com.rexcantor64.triton.bungeecord.packetinterceptor;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.bungeecord.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.bungeecord.utils.BaseComponentUtils;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.utils.ComponentUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.BossBar;
import net.md_5.bungee.protocol.packet.Chat;
import net.md_5.bungee.protocol.packet.Kick;
import net.md_5.bungee.protocol.packet.PlayerListHeaderFooter;
import net.md_5.bungee.protocol.packet.PlayerListItem;
import net.md_5.bungee.protocol.packet.PlayerListItemRemove;
import net.md_5.bungee.protocol.packet.PlayerListItemUpdate;
import net.md_5.bungee.protocol.packet.Subtitle;
import net.md_5.bungee.protocol.packet.SystemChat;
import net.md_5.bungee.protocol.packet.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BungeeListener extends MessageToMessageEncoder<DefinedPacket> {

    private final BungeeLanguagePlayer owner;
    private final int protocolVersion;

    private final HashMap<UUID, BaseComponent> tabListCache = new HashMap<>();

    public BungeeListener(BungeeLanguagePlayer owner, int protocolVersion) {
        this.owner = owner;
        this.protocolVersion = protocolVersion;
        owner.setListener(this);
    }

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private MainConfig config() {
        return Triton.get().getConfig();
    }

    private PlayerListItem.Item translatePlayerListItem(PlayerListItem.Item item) {
        if (item.getDisplayName() == null) {
            tabListCache.remove(item.getUuid());
            return item;
        }
        try {
            return parser()
                    .translateComponent(
                            BaseComponentUtils.deserialize(item.getDisplayName()),
                            owner,
                            config().getTabSyntax()
                    )
                    .map(BaseComponentUtils::serializeToSingle)
                    .mapToObj(
                            result -> {
                                PlayerListItem.Item clonedItem = clonePlayerListItem(item);
                                tabListCache.put(clonedItem.getUuid(), clonedItem.getDisplayName());
                                clonedItem.setDisplayName(result);
                                return clonedItem;
                            },
                            () -> {
                                tabListCache.remove(item.getUuid());
                                return item;
                            },
                            () -> {
                                tabListCache.remove(item.getUuid());
                                return item;
                            }
                    );
        } catch (Exception e) {
            e.printStackTrace();
            return item;
        }
    }

    private void handlePlayerListItem(DefinedPacket packet) {
        PlayerListItem p = (PlayerListItem) packet;
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (PlayerListItem.Item item : p.getItems()) {
            if (p.getAction() == PlayerListItem.Action.UPDATE_DISPLAY_NAME || p
                    .getAction() == PlayerListItem.Action.ADD_PLAYER) {
                items.add(translatePlayerListItem(item));
                continue;
            } else if (p.getAction() == PlayerListItem.Action.REMOVE_PLAYER) {
                tabListCache.remove(item.getUuid());
            }
            items.add(item);
        }
        p.setItems(items.toArray(new PlayerListItem.Item[0]));
    }

    private void handlePlayerListItemUpdate(DefinedPacket packet) {
        PlayerListItemUpdate playerListItemUpdatePacket = (PlayerListItemUpdate) packet;
        if (!playerListItemUpdatePacket.getActions().contains(PlayerListItemUpdate.Action.UPDATE_DISPLAY_NAME)) {
            return;
        }
        val items = Arrays.stream(playerListItemUpdatePacket.getItems())
                .map(this::translatePlayerListItem)
                .toArray(PlayerListItem.Item[]::new);
        playerListItemUpdatePacket.setItems(items);
    }

    private void handlePlayerListItemRemove(DefinedPacket packet) {
        PlayerListItemRemove playerListItemRemovePacket = (PlayerListItemRemove) packet;
        for (UUID uuid : playerListItemRemovePacket.getUuids()) {
            tabListCache.remove(uuid);
        }
    }

    private boolean handleChat(DefinedPacket packet) {
        Chat chatPacket = (Chat) packet;
        int type = chatPacket.getPosition();
        if ((type == 2 && !Triton.get().getConfig().isActionbars()) || (type != 2 && !Triton.get().getConfig().isChat())) {
            return true;
        }

        return !parser()
                .translateComponent(
                        ComponentUtils.deserializeFromJson(chatPacket.getMessage()),
                        owner,
                        type != 2 ? config().getChatSyntax() : config().getActionbarSyntax()
                )
                .map(result -> {
                    if (type == 2 && protocolVersion <= ProtocolConstants.MINECRAFT_1_10) {
                        // The Notchian client does not support true JSON messages on actionbars
                        // on 1.10 and below. Therefore, we must convert to a legacy string inside
                        // a TextComponent.
                        return ComponentSerializer.toString(new TextComponent(
                                ComponentSerializer.toString(BaseComponentUtils.serialize(result))
                        ));
                    } else {
                        return ComponentUtils.serializeToJson(result);
                    }
                })
                .ifChanged(chatPacket::setMessage)
                .isToRemove();
    }

    /**
     * Handles a system chat packet, added in Minecraft 1.19.
     *
     * @param packet A SystemChat packet.
     * @return True if the packet should be sent, false if it should be cancelled.
     */
    private boolean handleSystemChat(DefinedPacket packet) {
        SystemChat systemChatPacket = (SystemChat) packet;
        int type = systemChatPacket.getPosition();
        if ((type == 2 && !Triton.get().getConfig().isActionbars()) || (type != 2 && !Triton.get().getConfig().isChat())) {
            return true;
        }

        return !parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(systemChatPacket.getMessage()),
                        owner,
                        type != 2 ? config().getChatSyntax() : config().getActionbarSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(systemChatPacket::setMessage)
                .isToRemove();
    }

    private boolean handleTitle(DefinedPacket packet) {
        Title titlePacket = (Title) packet;
        if (titlePacket.getText() == null) {
            return true;
        }

        return !parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(titlePacket.getText()),
                        owner,
                        config().getTitleSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(titlePacket::setText)
                .isToRemove();
    }

    private boolean handleSubtitle(DefinedPacket packet) {
        Subtitle subtitlePacket = (Subtitle) packet;
        if (subtitlePacket.getText() == null) {
            return true;
        }

        return !parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(subtitlePacket.getText()),
                        owner,
                        config().getTitleSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(subtitlePacket::setText)
                .isToRemove();
    }

    private void handleBossbar(DefinedPacket packet) {
        BossBar p = (BossBar) packet;
        UUID uuid = p.getUuid();

        // Action 1 is "remove"
        if (p.getAction() == 1) {
            owner.removeBossbar(uuid);
            return;
        }

        // Action 0 is "add", action 3 is "update title"
        if (p.getAction() != 0 && p.getAction() != 3) {
            return;
        }
        owner.setBossbar(uuid, p.getTitle());

        parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(p.getTitle()),
                        owner,
                        config().getBossbarSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .map(BaseComponentUtils::serializeToSingle)
                .ifPresent(p::setTitle);
    }

    private void handlePlayerListHeaderFooter(DefinedPacket packet) {
        PlayerListHeaderFooter headerFooterPacket = (PlayerListHeaderFooter) packet;
        owner.setLastTabHeader(headerFooterPacket.getHeader());
        owner.setLastTabFooter(headerFooterPacket.getFooter());

        parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(headerFooterPacket.getHeader()),
                        owner,
                        config().getTabSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(headerFooterPacket::setHeader);
        parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(headerFooterPacket.getFooter()),
                        owner,
                        config().getTabSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(headerFooterPacket::setFooter);
    }

    private void handleKick(DefinedPacket packet) {
        Kick kickPacket = (Kick) packet;
        parser()
                .translateComponent(
                        BaseComponentUtils.deserialize(kickPacket.getMessage()),
                        owner,
                        config().getTabSyntax()
                )
                .map(BaseComponentUtils::serializeToSingle)
                .ifChanged(kickPacket::setMessage)
                .ifToRemove(() -> kickPacket.setMessage(new TextComponent()));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, DefinedPacket packet,
                          List<Object> out) {
        try {
            if (Triton.get().getConfig().isTab() && packet instanceof PlayerListItem) {
                handlePlayerListItem(packet);
            } else if (Triton.get().getConfig().isTab() && packet instanceof PlayerListItemUpdate) {
                handlePlayerListItemUpdate(packet);
            } else if (Triton.get().getConfig().isTab() && packet instanceof PlayerListItemRemove) {
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
        if (owner.getCurrentConnection() instanceof UserConnection) {
            ((UserConnection)owner.getCurrentConnection()).sendPacketQueued(packet);
        }
    }

    public void refreshTab() {
        List<PlayerListItem.Item> items = new ArrayList<>();
        for (Map.Entry<UUID, BaseComponent> item : tabListCache.entrySet()) {
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

    public void refreshBossbar(UUID uuid, BaseComponent text) {
        // BossBar was only added on MC 1.9
        if (owner.getParent().getPendingConnection().getVersion() < 107) {
            return;
        }
        BossBar p = new BossBar(uuid, 3);
        p.setTitle(text);
        send(p);
    }

    public void refreshTabHeaderFooter(BaseComponent header, BaseComponent footer) {
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

}
