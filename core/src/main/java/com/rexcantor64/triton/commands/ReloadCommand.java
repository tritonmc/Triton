package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReloadCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        val isBungee = event.getEnvironment() == CommandEvent.Environment.BUNGEE;

        sender.assertPermission("triton.reload", "multilanguageplugin.reload");

        if (isBungee) {
            val action = event.getArgs().length >= 1 && sender.getUUID() != null ? event.getArgs()[0] : "bungee";

            switch (action) {
                case "server":
                case "s":
                    Triton.asBungee().getBridgeManager().forwardCommand(event);
                    return true;
                case "all":
                case "a":
                    Triton.asBungee().getBridgeManager().forwardCommand(event);
                    break;
                case "bungee":
                case "b":
                    break;
                default:
                    sender.sendMessageFormatted("error.bungee-reload-invalid-mode", action);
                    return true;
            }
        }

        Triton.get().reload();
        sender.sendMessageFormatted(isBungee ? "success.bungee-reload" : "success.reload");
        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        if (event.getArgs().length > 1 || (!Triton.isBungee() && !Triton.get().getConfig().isBungeecord()))
            return Collections.emptyList();
        return Stream.of("server", "all", "bungee")
                .filter(v -> v.toLowerCase().startsWith(event.getArgs()[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
