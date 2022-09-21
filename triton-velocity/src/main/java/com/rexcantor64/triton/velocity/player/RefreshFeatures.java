package com.rexcantor64.triton.velocity.player;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import lombok.RequiredArgsConstructor;
import lombok.val;

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

    private void refreshBossBar(UUID uuid, String json) {
        val bossBarPacket = new BossBar();
        bossBarPacket.setUuid(uuid);
        bossBarPacket.setAction(BossBar.UPDATE_NAME);
        bossBarPacket.setName(json);

        sendPacket(bossBarPacket);
    }

    private void refreshPlayerListHeaderFooter() {
        val header = player.getLastTabHeader();
        val footer = player.getLastTabFooter();
        val headerFooterPacket = new HeaderAndFooter(header, footer);

        sendPacket(headerFooterPacket);
    }

    private void refreshPlayerListItems() {
        val items = player.getCachedPlayerListItems()
                .entrySet()
                .stream()
                .map(entry -> new PlayerListItem.Item(entry.getKey()).setDisplayName(entry.getValue()))
                .collect(Collectors.toList());
        val playerListItemsPacket = new PlayerListItem(PlayerListItem.UPDATE_DISPLAY_NAME, items);

        sendPacket(playerListItemsPacket);
    }

    private void sendPacket(MinecraftPacket packet) {
        ((ConnectedPlayer) player.getParent()).getConnection().write(packet);
    }

}
