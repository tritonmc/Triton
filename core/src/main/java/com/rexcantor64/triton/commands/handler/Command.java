package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;

import java.util.List;

public interface Command {

    void handleCommand(CommandEvent event) throws NoPermissionException, PlayerOnlyCommandException, UnsupportedPlatformException;

    List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException;

    default void assertPlayersOnly(CommandEvent event) throws PlayerOnlyCommandException {
        if (event.getSender().getUUID() == null) {
            throw new PlayerOnlyCommandException();
        }
    }

}
