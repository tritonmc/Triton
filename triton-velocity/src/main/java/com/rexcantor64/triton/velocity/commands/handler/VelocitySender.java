package com.rexcantor64.triton.velocity.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Sender;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.AllArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.UUID;

@AllArgsConstructor
public class VelocitySender implements Sender {
    private final CommandSource handler;
    private final LegacyComponentSerializer serializer = LegacyComponentSerializer.builder().character('&')
            .extractUrls().build();

    @Override
    public void sendMessage(String message) {
        this.sendMessage(serializer.deserialize(message));
    }

    @Override
    public void sendMessage(Component component) {
        handler.sendMessage(component);
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
        if (handler instanceof Player)
            return ((Player) handler).getUniqueId();
        return null;
    }
}