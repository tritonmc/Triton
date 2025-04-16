package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
public class TabPacketHandler {

    private final @NotNull AdventureParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public TabPacketHandler(@NotNull AdventureParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getTabSyntax();
    }

    public void onPlayerListHeaderAndFooterPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerPlayerListHeaderAndFooter(event);

        val originalHeader = packet.getHeader();
        val originalFooter = packet.getFooter();

        // header
        parser.translateComponent(
                        originalHeader,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setHeader(result);
                    event.markForReEncode(true);
                });
        // footer
        parser.translateComponent(
                        originalFooter,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setFooter(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            languagePlayer.getPacketEventsRefresh().savePlayerListHeaderFooter(originalHeader, originalFooter);
        } else {
            languagePlayer.getPacketEventsRefresh().discardPlayerListHeaderFooter();
        }
    }

    public void onPlayerInfoPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerPlayerInfo(event);

        val action = packet.getAction();

        if (action == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER || action == WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME) {
            for (WrapperPlayServerPlayerInfo.PlayerData entry : packet.getPlayerDataList()) {
                val uuid = entry.getUser().getUUID();
                val originalDisplayName = entry.getDisplayName();
                if (originalDisplayName == null) {
                    languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
                    continue;
                }

                parser.translateComponent(
                                originalDisplayName,
                                languagePlayer,
                                syntax
                        )
                        .ifChanged(result -> {
                            languagePlayer.getPacketEventsRefresh().savePlayerInfo(uuid, originalDisplayName);
                            entry.setDisplayName(result);
                            event.markForReEncode(true);
                        })
                        .ifUnchanged(() -> languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid))
                        .ifToRemove(() -> {
                            languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
                            entry.setDisplayName(null);
                            event.markForReEncode(true);
                        });
            }
        }

        if (action == WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER) {
            for (WrapperPlayServerPlayerInfo.PlayerData entry : packet.getPlayerDataList()) {
                languagePlayer.getPacketEventsRefresh().discardPlayerInfo(entry.getUser().getUUID());
            }
        }
    }

    public void onPlayerInfoUpdatePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerPlayerInfoUpdate(event);

        val actions = packet.getActions();
        if (!actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME)) {
            return;
        }

        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : packet.getEntries()) {
            val uuid = entry.getProfileId();
            val originalDisplayName = entry.getDisplayName();
            if (originalDisplayName == null) {
                languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
                continue;
            }

            parser.translateComponent(
                            originalDisplayName,
                            languagePlayer,
                            syntax
                    )
                    .ifChanged(result -> {
                        languagePlayer.getPacketEventsRefresh().savePlayerInfo(uuid, originalDisplayName);
                        entry.setDisplayName(result);
                        event.markForReEncode(true);
                    })
                    .ifUnchanged(() -> languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid))
                    .ifToRemove(() -> {
                        languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
                        entry.setDisplayName(null);
                        event.markForReEncode(true);
                    });
        }
    }

    public void onPlayerInfoRemovePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerPlayerInfoRemove(event);

        for (UUID uuid : packet.getProfileIds()) {
            languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
        }
    }
}
