package com.rexcantor64.triton.velocity.player;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfo;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RefreshFeatures {

    private final VelocityLanguagePlayer player;

    public synchronized void refreshAll() {
        refreshBossBars();
        refreshPlayerListHeaderFooter();
        refreshPlayerListItems();
    }

    private void refreshBossBars() {
        player.getCachedBossBars().forEach(this::refreshBossBar);
    }

    private void refreshBossBar(UUID uuid, Component component) {
        val bossBarPacket = new BossBar();
        bossBarPacket.setUuid(uuid);
        bossBarPacket.setAction(BossBar.UPDATE_NAME);
        bossBarPacket.setName(new ComponentHolder(player.getProtocolVersion(), component));

        sendPacket(bossBarPacket);
    }

    private void refreshPlayerListHeaderFooter() {
        val header = player.getLastTabHeader();
        val footer = player.getLastTabFooter();
        if (header == null || footer == null) {
            return;
        }
        val headerFooterPacket = new HeaderAndFooter(
                new ComponentHolder(player.getProtocolVersion(), header),
                new ComponentHolder(player.getProtocolVersion(), footer)
        );

        sendPacket(headerFooterPacket);
    }

    private void refreshPlayerListItems() {
        if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
            val items = player.getCachedPlayerListItems()
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        val playerEntry = new UpsertPlayerInfo.Entry(entry.getKey());
                        playerEntry.setDisplayName(new ComponentHolder(player.getProtocolVersion(), entry.getValue()));
                        return playerEntry;
                    })
                    .collect(Collectors.toList());
            val playerListItemsPacket = new UpsertPlayerInfo(EnumSet.of(UpsertPlayerInfo.Action.UPDATE_DISPLAY_NAME), items);

            sendPacket(playerListItemsPacket);
        } else {
            val items = player.getCachedPlayerListItems()
                    .entrySet()
                    .stream()
                    .map(entry -> new LegacyPlayerListItem.Item(entry.getKey()).setDisplayName(entry.getValue()))
                    .collect(Collectors.toList());
            val playerListItemsPacket = new LegacyPlayerListItem(LegacyPlayerListItem.UPDATE_DISPLAY_NAME, items);

            sendPacket(playerListItemsPacket);
        }
    }

    private void sendPacket(MinecraftPacket packet) {
        ((ConnectedPlayer) player.getParent()).getConnection().write(packet);
    }

}
