package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerDisconnect;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class DisconnectPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public DisconnectPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getKickSyntax();
    }

    public void onLoginDisconnectPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperLoginServerDisconnect(event);

        parser.translateComponent(
                        packet.getReason(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setReason(result);
                    event.markForReEncode(true);
                });
    }

    public void onConfigDisconnectPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperConfigServerDisconnect(event);

        parser.translateComponent(
                        packet.getReason(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setReason(result);
                    event.markForReEncode(true);
                });
    }

    public void onPlayDisconnectPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerDisconnect(event);

        parser.translateComponent(
                        packet.getReason(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setReason(result);
                    event.markForReEncode(true);
                });
    }
}
