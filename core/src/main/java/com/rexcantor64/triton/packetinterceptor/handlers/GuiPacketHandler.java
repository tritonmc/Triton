package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerOpenWindow;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class GuiPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;
    private final boolean translateGuiTitles;
    private final boolean translateItems;

    public GuiPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getGuiSyntax();
        this.translateGuiTitles = config.isGuis();
        this.translateItems = config.isItems();
    }

    public void onOpenWindowPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerOpenWindow(event);

        if (this.translateItems) {
            languagePlayer.getPacketEventsRefresh().setCurrentWindowId(packet.getContainerId());
        }

        if (translateGuiTitles) {
            parser.translateComponent(
                            packet.getTitle(),
                            languagePlayer,
                            syntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(result -> {
                        packet.setTitle(result);
                        event.markForReEncode(true);
                    });
        }
    }
}
