package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCombatEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDeathCombatEvent;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class DeathScreenPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public DeathScreenPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getDeathScreenSyntax();
    }

    public void onCombatEventPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerCombatEvent(event);

        packet.getDeathMessage()
                .flatMap(message -> parser.translateComponent(
                                        message,
                                        languagePlayer,
                                        syntax
                                )
                                .getResultOrToRemove(Component::empty)
                ).ifPresent(result -> {
                    packet.setDeathMessage(result);
                    event.markForReEncode(true);
                });
    }

    public void onDeathCombatEventPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerDeathCombatEvent(event);

        parser.translateComponent(
                        packet.getDeathMessage(),
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setDeathMessage(result);
                    event.markForReEncode(true);
                });
    }
}
