package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.CommandHandler;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class HelpCommand implements Command {

    private final CommandHandler commandHandler;

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException {
        event.getSender().assertPermission("triton.help", "multilanguageplugin.help");

        for (val str : Triton.get().getMessagesConfig().getMessageList("help.menu")) {
            if (str.equalsIgnoreCase("%1")) {
                for (String command : commandHandler.getAvailableCommands()) {
                    event.getSender().sendMessageFormatted("help.menu-item", event.getLabel(), command, Triton.get()
                            .getMessagesConfig()
                            .getMessage("command." + command));
                }
            } else {
                event.getSender().sendMessage(str);
            }
        }
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
