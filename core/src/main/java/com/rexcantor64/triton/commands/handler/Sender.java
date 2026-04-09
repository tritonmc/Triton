package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface Sender {

    @Deprecated
    void sendMessage(String message);

    void sendMessage(Component component);

    void sendMessageFormatted(String code, Object... args);

    void assertPermission(@NotNull String permission) throws NoPermissionException;

    boolean hasPermission(@NotNull String permission);

    UUID getUUID();

}
