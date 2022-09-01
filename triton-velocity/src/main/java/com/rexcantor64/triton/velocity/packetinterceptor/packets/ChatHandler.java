package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatBuilder;
import com.velocitypowered.proxy.protocol.packet.chat.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.SystemChat;
import net.kyori.adventure.text.Component;
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

    public @NotNull Optional<MinecraftPacket> handleSystemChat(@NotNull SystemChat systemChatPacket, @NotNull VelocityLanguagePlayer player) {
        boolean actionBar = systemChatPacket.getType() == ChatBuilder.ChatType.GAME_INFO;

        if ((!actionBar && shouldNotTranslateChat()) || (actionBar && shouldNotTranslateActionBars())) {
            return Optional.of(systemChatPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                systemChatPacket.getComponent(),
                                player,
                                actionBar ? getActionBarSyntax() : getChatSyntax()
                        )
                        .mapToObj(
                                result -> Optional.of(cloneSystemChatWithComponent(systemChatPacket, result)),
                                () -> Optional.of(systemChatPacket),
                                Optional::empty
                        )
        );
    }

    public @NotNull Optional<MinecraftPacket> handleLegacyChat(@NotNull LegacyChat legacyChatPacket, @NotNull VelocityLanguagePlayer player) {
        boolean actionBar = legacyChatPacket.getType() == 2; // action bar type is 2

        if ((!actionBar && shouldNotTranslateChat()) || (actionBar && shouldNotTranslateActionBars())) {
            return Optional.of(legacyChatPacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                ComponentUtils.deserializeFromJson(legacyChatPacket.getMessage()),
                                player,
                                actionBar ? getActionBarSyntax() : getChatSyntax()
                        )
                        .map(ComponentUtils::serializeToJson)
                        .mapToObj(
                                result -> Optional.of(cloneLegacyChatWithComponent(legacyChatPacket, result)),
                                () -> Optional.of(legacyChatPacket),
                                Optional::empty
                        )
        );
    }

    private @NotNull SystemChat cloneSystemChatWithComponent(@NotNull SystemChat systemChatPacket, Component newComponent) {
        return new SystemChat(newComponent, systemChatPacket.getType());
    }

    private @NotNull LegacyChat cloneLegacyChatWithComponent(@NotNull LegacyChat legacyChatPacket, String newMessage) {
        return new LegacyChat(newMessage, legacyChatPacket.getType(), legacyChatPacket.getSenderUuid());
    }

}
