package com.rexcantor64.triton.bungeecord.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.utils.BaseComponentUtils;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.Sender;
import lombok.AllArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

@AllArgsConstructor
public class BungeeSender implements Sender {
    private final CommandSender handler;

    @Override
    public void sendMessage(String message) {
        handler.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void sendMessage(Component component) {
        handler.sendMessage(BaseComponentUtils.serialize(component));
    }

    @Override
    public void sendMessageFormatted(String code, Object... args) {
        sendMessage(Triton.get().getMessagesConfig().getMessage(code, args));
    }

    @Override
    public void assertPermission(String... permissions) throws NoPermissionException {
        if (permissions.length == 0) {
            throw new NoPermissionException("");
        }

        for (val permission : permissions) {
            if (hasPermission(permission)) {
                return;
            }
        }
        throw new NoPermissionException(permissions[0]);
    }

    @Override
    public boolean hasPermission(String permission) {
        return handler.hasPermission(permission);
    }

    @Override
    public UUID getUUID() {
        if (handler instanceof ProxiedPlayer)
            return ((ProxiedPlayer) handler).getUniqueId();
        return null;
    }
}
