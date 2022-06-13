package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import lombok.val;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LogLevelCommand implements Command {
    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        sender.assertPermission("triton.loglevel");

        if (sender.getUUID() != null) {
            sender.sendMessageFormatted("error.only-console");
            return true;
        }

        if (event.getArgs().length == 0) {
            sender.sendMessageFormatted("success.current-loglevel", Triton.get().getConfig().getLogLevel());
            return true;
        }

        int newLogLevel;
        try {
            newLogLevel = Integer.parseInt(event.getArgs()[0]);
        } catch (NumberFormatException e) {
            sender.sendMessageFormatted("help.loglevel", event.getLabel());
            return true;
        }

        Triton.get().getConfig().setLogLevel(newLogLevel);

        sender.sendMessageFormatted("success.set-loglevel", newLogLevel);
        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        event.getSender().assertPermission("triton.loglevel");

        val possibilities = new String[]{"0", "1", "2"};

        if (event.getArgs().length > 1) return Collections.emptyList();

        if (event.getArgs().length == 0) {
            return Arrays.asList(possibilities);
        }

        return Arrays.stream(possibilities)
                .filter(logLevel -> logLevel.toLowerCase().startsWith(event.getArgs()[0]))
                .collect(Collectors.toList());
    }

}
