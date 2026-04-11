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

        sender.assertPermission("triton.reload");

        if (isProxy) {
            val action = event.getArgs().length >= 1 && sender.getUUID() != null ? event.getArgs()[0] : "proxy";

            switch (action) {
                case "server":
                case "s":
                    Triton.get().getBridgeManager().forwardCommand(event);
                    return;
                case "all":
                case "a":
                    Triton.get().getBridgeManager().forwardCommand(event);
                    break;
                case "proxy":
                case "p":
                case "bungee":
                case "b":
                case "velocity":
                case "v":
                    break;
                default:
                    sender.sendMessageFormatted("error.proxy-reload-invalid-mode", action);
                    return;
            }
        }

        Triton.get().reload();
        sender.sendMessageFormatted(isProxy ? "success.proxy-reload" : "success.reload");
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        if (event.getArgs().length > 1 || (!Triton.isProxy() && !Triton.get().getConfig().isBehindProxy()))
            return Collections.emptyList();
        return Stream.of("server", "all", "proxy")
                .filter(v -> v.toLowerCase().startsWith(event.getArgs()[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
