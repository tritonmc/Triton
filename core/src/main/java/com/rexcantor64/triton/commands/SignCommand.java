package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.plugin.Platform;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SignCommand implements Command {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, UnsupportedPlatformException {
        val sender = event.getSender();
        val uuid = sender.getUUID();

        if (uuid == null) {
            sender.sendMessage("Only players");
            return;
        }

        if (event.getPlatform() == Platform.SPIGOT) {
            throw new UnsupportedPlatformException();
        }

        // Command handler is overridden on the triton-spigot module
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException {
        event.getSender().assertPermission("triton.sign");

        val args = event.getArgs();

        if (args.length == 1)
            return Stream.of("set", "remove").filter((v) -> v.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());

        if (args.length == 2 && args[0].equalsIgnoreCase("set"))
            return Triton.get().getLanguageManager().getSignKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());

        return Collections.emptyList();
    }
}
