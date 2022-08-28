package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;

import java.util.UUID;

public interface Sender {

    void sendMessage(String message);

    void sendMessageFormatted(String code, Object... args);

    void assertPermission(String... permissions) throws NoPermissionException;

    boolean hasPermission(String permission);

    UUID getUUID();

}
