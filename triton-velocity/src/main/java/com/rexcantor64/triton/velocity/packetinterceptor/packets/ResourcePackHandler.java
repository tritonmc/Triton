package com.rexcantor64.triton.velocity.packetinterceptor.packets;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class ResourcePackHandler {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateResourcePack() {
        return !Triton.get().getConfig().isResourcePackPrompt();
    }


    private FeatureSyntax getResourcePackSyntax() {
        return Triton.get().getConfig().getResourcePackPromptSyntax();
    }


    public @NotNull Optional<MinecraftPacket> handleResourcePackRequest(@NotNull ResourcePackRequest resourcePackRequest, @NotNull VelocityLanguagePlayer player) {
        if (shouldNotTranslateResourcePack() || resourcePackRequest.getPrompt() == null) {
            return Optional.of(resourcePackRequest);
        }

        parser().translateComponent(
                        resourcePackRequest.getPrompt(),
                        player,
                        getResourcePackSyntax()
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(resourcePackRequest::setPrompt);
        return Optional.of(resourcePackRequest);
    }
}
