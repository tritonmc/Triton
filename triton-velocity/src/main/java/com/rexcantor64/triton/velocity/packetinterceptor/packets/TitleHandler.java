package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.utils.ComponentUtils;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.LegacyTitlePacket;
import com.velocitypowered.proxy.protocol.packet.title.TitleActionbarPacket;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class TitleHandler {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateTitles() {
        return !Triton.get().getConfig().isTitles();
    }

    private boolean shouldNotTranslateActionBars() {
        return !Triton.get().getConfig().isActionbars();
    }

    private FeatureSyntax getTitleSyntax() {
        return Triton.get().getConfig().getTitleSyntax();
    }

    private FeatureSyntax getActionBarSyntax() {
        return Triton.get().getConfig().getActionbarSyntax();
    }

    private boolean isActionBarPacket(GenericTitlePacket titlePacket) {
        if (titlePacket instanceof LegacyTitlePacket) {
            return titlePacket.getAction() == GenericTitlePacket.ActionType.SET_ACTION_BAR;
        }
        return titlePacket instanceof TitleActionbarPacket;
    }

    public @NotNull Optional<MinecraftPacket> handleGenericTitle(@NotNull GenericTitlePacket titlePacket, @NotNull VelocityLanguagePlayer player) {
        val isActionBarPacket = isActionBarPacket(titlePacket);
        if (isActionBarPacket ? shouldNotTranslateActionBars() : shouldNotTranslateTitles()) {
            return Optional.of(titlePacket);
        }

        return Objects.requireNonNull(
                parser().translateComponent(
                                ComponentUtils.deserializeFromJson(titlePacket.getComponent()),
                                player,
                                isActionBarPacket ? getActionBarSyntax() : getTitleSyntax()
                        )
                        .map(ComponentUtils::serializeToJson)
                        .mapToObj(
                                result -> {
                                    titlePacket.setComponent(result);
                                    return Optional.of(titlePacket);
                                },
                                () -> Optional.of(titlePacket),
                                Optional::empty
                        )
        );
    }

    public @NotNull Optional<MinecraftPacket> handleLegacyTitle(@NotNull LegacyTitlePacket titlePacket, @NotNull VelocityLanguagePlayer player) {
        val action = titlePacket.getAction();
        switch (action) {
            case SET_TITLE:
            case SET_SUBTITLE:
            case SET_ACTION_BAR:
                return handleGenericTitle(titlePacket, player);
            default:
                return Optional.empty();
        }
    }
}
