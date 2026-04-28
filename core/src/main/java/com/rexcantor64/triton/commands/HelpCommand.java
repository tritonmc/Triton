package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.CommandHandler;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class HelpCommand implements Command {

    private final CommandHandler commandHandler;

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException {
        event.getSender().assertPermission("triton.help");

        val commands = commandHandler.getAvailableCommands()
                .stream()
                .map(name -> {
                    val description = Triton.get().getMessagesConfig().getMessageComponent("command." + name);
                    return Triton.get().getMessagesConfig().getMessageComponent(
                            "help.menu-item",
                            Component.text(event.getLabel()),
                            Component.text(name),
                            description
                    );
                })
                .collect(Component.toComponent(Component.newline()));
        event.getSender().sendMessageFormatted("help.menu", commands);
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
