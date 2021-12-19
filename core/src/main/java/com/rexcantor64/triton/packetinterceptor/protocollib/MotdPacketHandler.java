package com.rexcantor64.triton.packetinterceptor.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class MotdPacketHandler extends PacketAdapter {

    public MotdPacketHandler() {
        super(Triton.asSpigot().getLoader(), ListenerPriority.HIGHEST,
                Collections.singleton(PacketType.Status.Server.SERVER_INFO), ListenerOptions.ASYNC);
    }

    /**
     * @return Whether the plugin should attempt to translate the MOTD
     */
    private boolean isMotdEnabled() {
        return Triton.get().getConf().isMotd();
    }

    /**
     * Handle a Server Info (MOTD) packet.
     * Placeholders are searched in the text itself, as well as in the ping message.
     * The resulting components are flattened as legacy text using {@link ComponentUtils#mergeComponents(BaseComponent...)}.
     *
     * @param event ProtocolLib's packet event
     */
    private void handleServerInfo(PacketEvent event) {
        val lang = Triton.get().getStorage().getLanguageFromIp(Objects
                .requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress()).getName();
        val syntax = Triton.get().getConfig().getMotdSyntax();

        val serverPing = event.getPacket().getServerPings().readSafely(0);
        serverPing.setPlayers(serverPing.getPlayers().stream().map((gp) -> {
            if (gp.getName() == null) return gp;
            val translatedName = Triton.get().getLanguageParser().replaceLanguages(gp.getName(), lang, syntax);
            if (gp.getName().equals(translatedName)) return gp;
            if (translatedName == null) return null;
            return gp.withName(translatedName);
        }).filter(Objects::nonNull).collect(Collectors.toList()));

        val motd = serverPing.getMotD();
        val result = Triton.get().getLanguageParser()
                .parseComponent(lang, syntax, ComponentSerializer.parse(motd.getJson()));
        if (result != null)
            motd.setJson(ComponentSerializer.toString(ComponentUtils.mergeComponents(result)));
        serverPing.setMotD(motd);
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) return;
        if (packet.getPacketType() == PacketType.Status.Server.SERVER_INFO && isMotdEnabled()) {
            handleServerInfo(packet);
        }
    }
}
