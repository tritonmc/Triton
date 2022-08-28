package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.plugin.Platform;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class OpenSelectorCommand implements Command {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, UnsupportedPlatformException {
        val sender = event.getSender();
        val uuid = sender.getUUID();

        if (uuid == null) {
            sender.sendMessage("Only players");
            return;
        }

        if (event.getPlatform() != Platform.SPIGOT) {
            throw new UnsupportedPlatformException();
        }

        // Command handler is overridden on the triton-spigot module
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
