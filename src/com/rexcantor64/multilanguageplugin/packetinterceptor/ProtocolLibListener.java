package com.rexcantor64.multilanguageplugin.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.components.api.chat.BaseComponent;
import com.rexcantor64.multilanguageplugin.components.chat.ComponentSerializer;
import org.bukkit.plugin.Plugin;

public class ProtocolLibListener implements PacketListener {

    private SpigotMLP main;

    public ProtocolLibListener(SpigotMLP main) {
        this.main = main;
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        if (packet.getPacketType() == PacketType.Play.Server.CHAT) {
            EnumWrappers.ChatType type = packet.getPacket().getChatTypes().read(0);
            if (type == EnumWrappers.ChatType.GAME_INFO && main.getConf().isActionbars()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseActionbar(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
                packet.getPacket().getChatComponents().write(0, msg);
            } else if (type != EnumWrappers.ChatType.GAME_INFO && main.getConf().isChat()) {
                WrappedChatComponent msg = packet.getPacket().getChatComponents().read(0);
                if (msg != null) {
                    msg.setJson(ComponentSerializer.toString(main.getLanguageParser().parseChat(packet.getPlayer(), ComponentSerializer.parse(msg.getJson()))));
                    packet.getPacket().getChatComponents().write(0, msg);
                    return;
                }
                packet.getPacket().getModifier().write(1, toLegacy(main.getLanguageParser().parseChat(packet.getPlayer(), fromLegacy((net.md_5.bungee.api.chat.BaseComponent[]) packet.getPacket().getModifier().read(1)))));
            }
        }
    }

    @Override
    public void onPacketReceiving(PacketEvent packetEvent) {

    }

    @Override
    public ListeningWhitelist getSendingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Server.CHAT).build();
    }

    @Override
    public ListeningWhitelist getReceivingWhitelist() {
        return ListeningWhitelist.newBuilder().gamePhase(GamePhase.PLAYING).types(PacketType.Play.Client.SETTINGS).build();
    }

    @Override
    public Plugin getPlugin() {
        return main;
    }

    private BaseComponent[] fromLegacy(net.md_5.bungee.api.chat.BaseComponent[] components) {
        return ComponentSerializer.parse(net.md_5.bungee.chat.ComponentSerializer.toString(components));
    }

    private net.md_5.bungee.api.chat.BaseComponent[] toLegacy(BaseComponent[] components) {
        return net.md_5.bungee.chat.ComponentSerializer.parse(ComponentSerializer.toString(components));
    }
}
