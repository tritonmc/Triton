package com.rexcantor64.triton.velocity.player;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket;
import com.velocitypowered.proxy.protocol.packet.LegacyPlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.UpsertPlayerInfoPacket;
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
        val bossBarPacket = new BossBarPacket();
        bossBarPacket.setUuid(uuid);
        bossBarPacket.setAction(BossBarPacket.UPDATE_NAME);
        bossBarPacket.setName(new ComponentHolder(player.getProtocolVersion(), component));

        sendPacket(bossBarPacket);
    }

    private void refreshPlayerListHeaderFooter() {
        val header = player.getLastTabHeader();
        val footer = player.getLastTabFooter();
        if (header == null || footer == null) {
            return;
        }
        val headerFooterPacket = new HeaderAndFooterPacket(
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
                        val playerEntry = new UpsertPlayerInfoPacket.Entry(entry.getKey());
                        playerEntry.setDisplayName(new ComponentHolder(player.getProtocolVersion(), entry.getValue()));
                        return playerEntry;
                    })
                    .collect(Collectors.toList());
            val playerListItemsPacket = new UpsertPlayerInfoPacket(EnumSet.of(UpsertPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME), items);

            sendPacket(playerListItemsPacket);
        } else {
            val items = player.getCachedPlayerListItems()
                    .entrySet()
                    .stream()
                    .map(entry -> new LegacyPlayerListItemPacket.Item(entry.getKey()).setDisplayName(entry.getValue()))
                    .collect(Collectors.toList());
            val playerListItemsPacket = new LegacyPlayerListItemPacket(LegacyPlayerListItemPacket.UPDATE_DISPLAY_NAME, items);

            sendPacket(playerListItemsPacket);
        }
    }

    private void sendPacket(MinecraftPacket packet) {
        ((ConnectedPlayer) player.getParent()).getConnection().write(packet);
    }

}
