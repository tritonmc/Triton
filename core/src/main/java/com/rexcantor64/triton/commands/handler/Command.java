package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;

import java.util.List;

public interface Command {

    void handleCommand(CommandEvent event) throws NoPermissionException, UnsupportedPlatformException;

    List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException;

}
