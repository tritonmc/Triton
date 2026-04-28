package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;

public interface Sender {

    @Deprecated
    void sendMessage(String message);

    void sendMessage(Component component);

    default void sendMessageFormatted(String code, Object... args) {
        this.sendMessageFormatted(code, Arrays.stream(args).map(obj -> {
            if (obj instanceof ComponentLike) {
                return (ComponentLike) obj;
            }
            return Component.text(obj.toString());
        }).toArray(ComponentLike[]::new));
    }

    void sendMessageFormatted(String code, ComponentLike... args);

    void assertPermission(@NotNull String permission) throws NoPermissionException;

    boolean hasPermission(@NotNull String permission);

    UUID getUUID();

}
