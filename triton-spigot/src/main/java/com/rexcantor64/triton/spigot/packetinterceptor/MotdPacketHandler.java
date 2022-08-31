package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.spigot.utils.WrappedComponentUtils;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MotdPacketHandler extends PacketAdapter {

    public MotdPacketHandler() {
        super(SpigotTriton.asSpigot().getLoader(), ListenerPriority.HIGHEST,
                Collections.singleton(PacketType.Status.Server.SERVER_INFO), ListenerOptions.ASYNC);
    }

    /**
     * Alias of <code>Triton.get().getMessageParser()</code>.
     *
     * @see Triton#getMessageParser()
     */
    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    /**
     * @return Whether the plugin should attempt to translate the MOTD
     */
    private boolean isMotdEnabled() {
        return Triton.get().getConfig().isMotd();
    }

    /**
     * Handle a Server Info (MOTD) packet.
     * Placeholders are searched in the text itself, as well as in the ping message.
     *
     * @param event ProtocolLib's packet event
     */
    private void handleServerInfo(PacketEvent event) {
        val lang = Triton.get().getStorage().getLanguageFromIp(
                Objects.requireNonNull(event.getPlayer().getAddress()).getAddress().getHostAddress()
        );
        val syntax = Triton.get().getConfig().getMotdSyntax();

        val serverPing = event.getPacket().getServerPings().readSafely(0);
        serverPing.setPlayers(serverPing.getPlayers().stream().flatMap((gp) -> {
            if (gp.getName() == null) {
                return Stream.of(gp);
            }

            return parser()
                    .translateString(
                            gp.getName(),
                            lang,
                            syntax
                    )
                    .mapToObj(
                            (translatedName) -> {
                                val translatedNameSplit = translatedName.split("\n", -1);
                                if (translatedNameSplit.length > 1) {
                                    return Arrays.stream(translatedNameSplit).map(name -> new WrappedGameProfile(UUID.randomUUID(), name));
                                } else {
                                    return Stream.of(gp.withName(translatedName));
                                }
                            },
                            () -> Stream.of(gp),
                            Stream::empty
                    );

        }).collect(Collectors.toList()));

        parser().translateString(serverPing.getVersionName(), lang, syntax)
                .ifChanged(serverPing::setVersionName);

        parser()
                .translateComponent(
                        WrappedComponentUtils.deserialize(serverPing.getMotD()),
                        lang,
                        syntax
                )
                .map(WrappedComponentUtils::serialize)
                .ifChanged(serverPing::setMotD)
                .ifToRemove(() -> serverPing.setMotD(WrappedComponentUtils.serialize(Component.empty())));
    }

    @Override
    public void onPacketSending(PacketEvent packet) {
        if (!packet.isServerPacket()) {
            return;
        }

        if (packet.getPacketType() == PacketType.Status.Server.SERVER_INFO && isMotdEnabled()) {
            handleServerInfo(packet);
        }
    }
}
