package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTitle;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class TitlePacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax titleSyntax;
    private final @NotNull MainConfig.FeatureSyntax actionbarSyntax;
    private final boolean translateTitles;
    private final boolean translateActionbars;

    public TitlePacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.titleSyntax = config.getTitleSyntax();
        this.actionbarSyntax = config.getActionbarSyntax();
        this.translateTitles = config.isTitles();
        this.translateActionbars = config.isActionbars();
    }

    public void onTitlePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerTitle(event);

        if (translateTitles && packet.getAction() == WrapperPlayServerTitle.TitleAction.SET_TITLE) {
            parser.translateComponent(
                            packet.getTitle(),
                            languagePlayer,
                            titleSyntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(result -> {
                        packet.setTitle(result);
                        event.markForReEncode(true);
                    });
        } else if (translateTitles && packet.getAction() == WrapperPlayServerTitle.TitleAction.SET_SUBTITLE) {
            parser.translateComponent(
                            packet.getSubtitle(),
                            languagePlayer,
                            titleSyntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(result -> {
                        packet.setSubtitle(result);
                        event.markForReEncode(true);
                    });
        } else if (translateActionbars && packet.getAction() == WrapperPlayServerTitle.TitleAction.SET_ACTION_BAR) {
            // No idea why actionbars are included in this packet...
            parser.translateComponent(
                            packet.getActionBar(),
                            languagePlayer,
                            actionbarSyntax
                    )
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(result -> {
                        packet.setActionBar(result);
                        event.markForReEncode(true);
                    });
        }
    }

    public void onSetTitleTextPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSetTitleText(event);

        parser.translateComponent(
                        packet.getTitle(),
                        languagePlayer,
                        titleSyntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setTitle(result);
                    event.markForReEncode(true);
                });
    }

    public void onSetTitleSubtitlePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerSetTitleSubtitle(event);

        parser.translateComponent(
                        packet.getSubtitle(),
                        languagePlayer,
                        titleSyntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setSubtitle(result);
                    event.markForReEncode(true);
                });
    }
}
