package com.rexcantor64.triton.bungeecord.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.utils.BaseComponentUtils;
import com.rexcantor64.triton.commands.handler.Sender;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@AllArgsConstructor
public class BungeeSender implements Sender {
    private final CommandSender handler;

    @Override
    @Deprecated
    public void sendMessage(String message) {
        handler.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void sendMessage(Component component) {
        handler.sendMessage(BaseComponentUtils.serialize(component));
    }

    @Override
    public void sendMessageFormatted(String code, ComponentLike... args) {
        sendMessage(Triton.get().getMessagesConfig().getMessageComponent(code, args));
    }

    @Override
    public void assertPermission(@NotNull String permission) throws NoPermissionException {
        if (!hasPermission(permission)) {
            throw new NoPermissionException(permission);
        }
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return handler.hasPermission(permission);
    }

    @Override
    public UUID getUUID() {
        if (handler instanceof ProxiedPlayer)
            return ((ProxiedPlayer) handler).getUniqueId();
        return null;
    }
}
