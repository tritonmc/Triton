package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.rexcantor64.triton.velocity.utils.ComponentUtils;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatType;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChatPacket;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class ChatHandler {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateChat() {
        return !Triton.get().getConfig().isChat();
    }

    private boolean shouldNotTranslateActionBars() {
        return !Triton.get().getConfig().isActionbars();
    }

    private FeatureSyntax getChatSyntax() {
        return Triton.get().getConfig().getChatSyntax();
    }

    private FeatureSyntax getActionBarSyntax() {
        return Triton.get().getConfig().getActionbarSyntax();
    }

    public @NotNull Optional<MinecraftPacket> handleSystemChat(@NotNull SystemChatPacket systemChatPacket, @NotNull VelocityLanguagePlayer player) {
        boolean actionBar = systemChatPacket.getType() == ChatType.GAME_INFO;

        if ((!actionBar && shouldNotTranslateChat()) || (actionBar && shouldNotTranslateActionBars())) {
            return Optional.of(systemChatPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                systemChatPacket.getComponent().getComponent(),
                                player,
                                actionBar ? getActionBarSyntax() : getChatSyntax()
                        )
                        .map(result -> new ComponentHolder(player.getProtocolVersion(), result))
                        .mapToObj(
                                result -> Optional.of(cloneSystemChatWithComponent(systemChatPacket, result)),
                                () -> Optional.of(systemChatPacket),
                                Optional::empty
                        )
        );
    }

    public @NotNull Optional<MinecraftPacket> handleLegacyChat(@NotNull LegacyChatPacket legacyChatPacket, @NotNull VelocityLanguagePlayer player) {
        boolean actionBar = legacyChatPacket.getType() == 2; // action bar type is 2

        if ((!actionBar && shouldNotTranslateChat()) || (actionBar && shouldNotTranslateActionBars())) {
            return Optional.of(legacyChatPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                ComponentUtils.deserializeFromJson(legacyChatPacket.getMessage(), player.getProtocolVersion()),
                                player,
                                actionBar ? getActionBarSyntax() : getChatSyntax()
                        )
                        .map(result -> ComponentUtils.serializeToJson(result, player.getProtocolVersion(), actionBar))
                        .mapToObj(
                                result -> Optional.of(cloneLegacyChatWithComponent(legacyChatPacket, result)),
                                () -> Optional.of(legacyChatPacket),
                                Optional::empty
                        )
        );
    }

    private @NotNull SystemChatPacket cloneSystemChatWithComponent(@NotNull SystemChatPacket systemChatPacket, ComponentHolder newComponent) {
        return new SystemChatPacket(newComponent, systemChatPacket.getType());
    }

    private @NotNull LegacyChatPacket cloneLegacyChatWithComponent(@NotNull LegacyChatPacket legacyChatPacket, String newMessage) {
        return new LegacyChatPacket(newMessage, legacyChatPacket.getType(), legacyChatPacket.getSenderUuid());
    }

}
