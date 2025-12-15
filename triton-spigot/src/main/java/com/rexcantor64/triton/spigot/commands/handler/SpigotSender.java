package com.rexcantor64.triton.spigot.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Sender;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.spigot.utils.BaseComponentUtils;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        if (handler instanceof Player)
            return ((Player) handler).getUniqueId();
        return null;
    }
}
