package com.rexcantor64.triton.velocity.player;

import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.UUID;

@RequiredArgsConstructor
public class RefreshFeatures {

    private final VelocityLanguagePlayer player;

    public synchronized void refreshAll() {
        refreshBossBars();
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

    private void sendPacket(MinecraftPacket packet) {
        ((ConnectedPlayer) player.getParent()).getConnection().write(packet);
    }

}
