package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.PacketEventsRefresh;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class TabPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;
    private final @NotNull MainConfig.FeatureSyntax entitiesSyntax;
    private final boolean translateTab;
    private final boolean translatePlayers;

    public TabPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config, boolean translatePlayers) {
        this.parser = parser;
        this.syntax = config.getTabSyntax();
        this.entitiesSyntax = config.getHologramSyntax();
        this.translateTab = config.isTab();
        this.translatePlayers = translatePlayers;
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

        val isAdd = action == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER;
        if (this.translatePlayers && action != WrapperPlayServerPlayerInfo.Action.REMOVE_PLAYER) {
            for (val entry : packet.getPlayerDataList()) {
                // This is only useful for NPCs, since their name can only be changed through the UserProfile
                val profile = Objects.requireNonNull(entry.getUserProfile(), "user profile must not be null");
                Optional<PacketEventsRefresh.Player> playerOpt;
                if (isAdd) {
                    playerOpt = saveAndTranslateUserProfile(event, languagePlayer, profile);
                } else {
                    playerOpt = languagePlayer.getPacketEventsRefresh().getTentativePlayer(profile.getUUID());
                }
                // save data for refreshes
                playerOpt.ifPresent(player -> {
                    if (isAdd || action == WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME) {
                        player.setDisplayName(entry.getDisplayName());
                    }
                    if (isAdd || action == WrapperPlayServerPlayerInfo.Action.UPDATE_LATENCY) {
                        player.setLatency(entry.getPing());
                    }
                });
            }
        }

        if (this.translateTab && (isAdd || action == WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME)) {
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
                val uuid = entry.getUser().getUUID();
                if (this.translateTab) {
                    languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
                }
                if (this.translatePlayers) {
                    languagePlayer.getPacketEventsRefresh().discardTentativePlayer(uuid);
                }
            }
        }
    }

    public void onPlayerInfoUpdatePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerPlayerInfoUpdate(event);

        val actions = packet.getActions();
        val isUpdateDisplayName = actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME);
        val isUpdateDisplayNameTab = this.translateTab && isUpdateDisplayName;
        if (!this.translatePlayers && !isUpdateDisplayNameTab) {
            return;
        }

        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : packet.getEntries()) {
            val uuid = entry.getProfileId();
            if (this.translatePlayers) {
                Optional<PacketEventsRefresh.Player> playerOpt;
                if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER)) {
                    // This is only useful for NPCs, since their name can only be changed through the UserProfile
                    playerOpt = saveAndTranslateUserProfile(event, languagePlayer, entry.getGameProfile());
                } else {
                    playerOpt = languagePlayer.getPacketEventsRefresh().getTentativePlayer(uuid);
                }
                // save data for refreshes
                playerOpt.ifPresent(player -> {
                    if (isUpdateDisplayName) {
                        player.setDisplayName(entry.getDisplayName());
                    }
                    if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LATENCY)) {
                        player.setLatency(entry.getLatency());
                    }
                    if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED)) {
                        player.setListed(entry.isListed());
                    }
                    if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LIST_ORDER)) {
                        player.setListOrder(entry.getListOrder());
                    }
                    if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_HAT)) {
                        player.setShowHat(entry.isShowHat());
                    }
                });
            }

            if (!isUpdateDisplayNameTab) {
                continue;
            }
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
            if (this.translateTab) {
                languagePlayer.getPacketEventsRefresh().discardPlayerInfo(uuid);
            }
            if (this.translatePlayers) {
                languagePlayer.getPacketEventsRefresh().discardTentativePlayer(uuid);
            }
        }
    }

    private Optional<PacketEventsRefresh.Player> saveAndTranslateUserProfile(@NotNull PacketSendEvent event,
                                                                             @NotNull TritonLanguagePlayer<?> languagePlayer,
                                                                             @NotNull UserProfile profile
    ) {
        val uuid = profile.getUUID();
        val name = profile.getName();
        return parser.translateString(name, languagePlayer, this.entitiesSyntax)
                .ifUnchanged(() -> languagePlayer.getPacketEventsRefresh().discardTentativePlayer(uuid))
                .getResultOrToRemove(String::new)
                .map(result -> {
                    val player = languagePlayer.getPacketEventsRefresh().saveTentativePlayer(uuid, name);
                    player.setTextureProperties(profile.getTextureProperties());
                    // player names are limited to 16 characters (packetevents truncates automatically)...
                    // seriously, just use proper holograms instead
                    profile.setName(result);
                    event.markForReEncode(true);
                    return player;
                });
    }
}
