package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReloadCommand implements Command {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException {
        val sender = event.getSender();
        val isProxy = event.getPlatform().isProxy();

        sender.assertPermission("triton.reload", "multilanguageplugin.reload");

        if (isProxy) {
            val action = event.getArgs().length >= 1 && sender.getUUID() != null ? event.getArgs()[0] : "bungee";

            switch (action) {
                case "server":
                case "s":
                    Triton.get().getBridgeManager().forwardCommand(event);
                    return;
                case "all":
                case "a":
                    Triton.get().getBridgeManager().forwardCommand(event);
                    break;
                case "bungee":
                case "b":
                    break;
                default:
                    sender.sendMessageFormatted("error.bungee-reload-invalid-mode", action);
                    return;
            }
        }

        Triton.get().reload();
        sender.sendMessageFormatted(isProxy ? "success.bungee-reload" : "success.reload");
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        if (event.getArgs().length > 1 || (!Triton.isProxy() && !Triton.get().getConfig().isBungeecord()))
            return Collections.emptyList();
        return Stream.of("server", "all", "bungee")
                .filter(v -> v.toLowerCase().startsWith(event.getArgs()[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
