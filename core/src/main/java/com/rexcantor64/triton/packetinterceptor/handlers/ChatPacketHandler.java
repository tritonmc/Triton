package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ChatPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax chatSyntax;
    private final @NotNull MainConfig.FeatureSyntax actionbarSyntax;
    private final boolean translateChat;
    private final boolean translateActionbars;

    public ChatPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.chatSyntax = config.getChatSyntax();
        this.actionbarSyntax = config.getActionbarSyntax();
        this.translateChat = config.isChat();
        this.translateActionbars = config.isActionbars();
    }

    public void onChatMessagePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerChatMessage(event);
        val message = packet.getMessage();

        @SuppressWarnings("deprecation")
        val isActionbar = message.getType() == ChatTypes.GAME_INFO;
        if (!(isActionbar && translateActionbars) && !(!isActionbar && translateChat)) {
            return;
        }

        parser.translateComponent(
                        message.getChatContent(),
                        languagePlayer,
                        isActionbar ? actionbarSyntax : chatSyntax
                )
                .ifChanged(result -> {
                    message.setChatContent(result);
                    event.markForReEncode(true);
                })
                .ifToRemove(() -> event.setCancelled(true));
    }

    public void onSystemChatMessagePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSystemChatMessage(event);

        @SuppressWarnings("deprecation")
        val isActionbar = packet.getType() == ChatTypes.GAME_INFO || packet.isOverlay();
        if (!(isActionbar && translateActionbars) && !(!isActionbar && translateChat)) {
            return;
        }

        parser.translateComponent(
                        packet.getMessage(),
                        languagePlayer,
                        isActionbar ? actionbarSyntax : chatSyntax
                )
                .ifChanged(result -> {
                    packet.setMessage(result);
                    event.markForReEncode(true);
                })
                .ifToRemove(() -> event.setCancelled(true));
    }

}
