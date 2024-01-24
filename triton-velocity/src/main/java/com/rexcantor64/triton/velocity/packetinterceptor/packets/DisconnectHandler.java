package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class DisconnectHandler {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateKick() {
        return !Triton.get().getConfig().isKick();
    }

    private FeatureSyntax getKickSyntax() {
        return Triton.get().getConfig().getKickSyntax();
    }

    public @NotNull Optional<MinecraftPacket> handleDisconnect(@NotNull DisconnectPacket disconnectPacket, @NotNull VelocityLanguagePlayer player) {
        if (shouldNotTranslateKick()) {
            return Optional.of(disconnectPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                disconnectPacket.getReason().getComponent(),
                                player,
                                getKickSyntax()
                        )
                        .map(result -> new ComponentHolder(player.getProtocolVersion(), result))
                        .mapToObj(
                                result -> {
                                    disconnectPacket.setReason(result);
                                    return Optional.of(disconnectPacket);
                                },
                                () -> Optional.of(disconnectPacket),
                                Optional::empty
                        )
        );
    }

}
