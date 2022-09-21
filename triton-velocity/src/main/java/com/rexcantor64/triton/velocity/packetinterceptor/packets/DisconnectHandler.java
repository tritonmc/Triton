package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.rexcantor64.triton.velocity.utils.ComponentUtils;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
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

    public @NotNull Optional<MinecraftPacket> handleDisconnect(@NotNull Disconnect disconnectPacket, @NotNull VelocityLanguagePlayer player) {
        if (shouldNotTranslateKick()) {
            return Optional.of(disconnectPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                ComponentUtils.deserializeFromJson(disconnectPacket.getReason(), player.getProtocolVersion()),
                                player,
                                getKickSyntax()
                        )
                        .map(result -> ComponentUtils.serializeToJson(result, player.getProtocolVersion()))
                        .mapToObj(
                                result -> Optional.of(new Disconnect(result)),
                                () -> Optional.of(disconnectPacket),
                                Optional::empty
                        )
        );
    }

}
