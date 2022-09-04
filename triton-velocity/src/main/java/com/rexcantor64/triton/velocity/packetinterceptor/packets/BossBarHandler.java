package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BossBarHandler {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateBossBars() {
        return !Triton.get().getConfig().isBossbars();
    }


    private FeatureSyntax getBossBarSyntax() {
        return Triton.get().getConfig().getBossbarSyntax();
    }


    public @NotNull Optional<MinecraftPacket> handleBossBar(@NotNull BossBar bossBarPacket, @NotNull VelocityLanguagePlayer player) {
        if (shouldNotTranslateBossBars()) {
            return Optional.of(bossBarPacket);
        }

        val uuid = bossBarPacket.getUuid();
        val action = bossBarPacket.getAction();

        if (action == BossBar.REMOVE) {
            player.removeBossbar(uuid);
            return Optional.of(bossBarPacket);
        }

        val text = bossBarPacket.getName();
        if (text != null && (action == BossBar.ADD || action == BossBar.UPDATE_NAME)) {
            player.setBossbar(uuid, bossBarPacket.getName());

            parser()
                    .translateComponent(
                            ComponentUtils.deserializeFromJson(bossBarPacket.getName()),
                            player,
                            getBossBarSyntax()
                    )
                    .getResultOrToRemove(Component::empty)
                    .map(ComponentUtils::serializeToJson)
                    .ifPresent(bossBarPacket::setName);
        }
        return Optional.of(bossBarPacket);
    }
}
