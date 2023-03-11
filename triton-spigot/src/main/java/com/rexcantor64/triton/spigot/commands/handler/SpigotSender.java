package com.rexcantor64.triton.spigot.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Sender;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.spigot.utils.BaseComponentUtils;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.AllArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

@AllArgsConstructor
public class SpigotSender implements Sender {
    private final CommandSender handler;

    @Override
    public void sendMessage(String message) {
        handler.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void sendMessage(Component component) {
        handler.spigot().sendMessage(BaseComponentUtils.serialize(component));
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
