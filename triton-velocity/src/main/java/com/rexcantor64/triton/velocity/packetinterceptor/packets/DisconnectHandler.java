package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import lombok.val;
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
                        .map(result -> {
                            // During the Login phase, this packet is supposed to send JSON text even on 1.20.3+ (instead of NBT data)
                            // https://github.com/PaperMC/Velocity/blob/be678840de9c927c9e17bcea06ed7aedcea77d1e/proxy/src/main/java/com/velocitypowered/proxy/protocol/packet/DisconnectPacket.java#L81-L82
                            val connectedPlayer = (ConnectedPlayer) player.getParent();
                            val isLoginPhase = connectedPlayer.getConnection().getState() == StateRegistry.LOGIN;
                            return new ComponentHolder(
                                    isLoginPhase ? ProtocolVersion.MINECRAFT_1_20_2 : player.getProtocolVersion(),
                                    result
                            );
                        })
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
