package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerActionBar;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ActionBarPacketHandler {

    private final @NotNull AdventureParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public ActionBarPacketHandler(@NotNull AdventureParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getActionbarSyntax();
    }

    public void onActionBarPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerActionBar(event);

        parser.translateComponent(
                        packet.getActionBarText(),
                        languagePlayer,
                        syntax
                )
                .ifChanged(result -> {
                    packet.setActionBarText(result);
                    event.markForReEncode(true);
                })
                .ifToRemove(() -> event.setCancelled(true));
    }
}
