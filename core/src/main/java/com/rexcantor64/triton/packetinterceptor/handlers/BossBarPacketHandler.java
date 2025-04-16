package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBossBar;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class BossBarPacketHandler {

    private final @NotNull AdventureParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public BossBarPacketHandler(@NotNull AdventureParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getBossbarSyntax();
    }

    public void onBossBarPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerBossBar(event);

        val uuid = packet.getUUID();
        val action = packet.getAction();

        if (action == WrapperPlayServerBossBar.Action.REMOVE) {
            languagePlayer.getPacketEventsRefresh().discardBossBar(uuid);
            return;
        }

        if (action != WrapperPlayServerBossBar.Action.ADD && action != WrapperPlayServerBossBar.Action.UPDATE_TITLE) {
            return;
        }

        val originalTitle = packet.getTitle();

        parser.translateComponent(
                        originalTitle,
                        languagePlayer,
                        syntax
                )
                .ifChanged(result -> languagePlayer.getPacketEventsRefresh().saveBossBar(uuid, originalTitle))
                .ifToRemove(() -> languagePlayer.getPacketEventsRefresh().discardBossBar(uuid))
                .ifUnchanged(() -> languagePlayer.getPacketEventsRefresh().discardBossBar(uuid))
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setTitle(result);
                    event.markForReEncode(true);
                });
    }
}
