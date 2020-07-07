package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.CommandHandler;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class HelpCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        event.getSender().assertPermission("triton.help", "multilanguageplugin.help");

        for (val str : Triton.get().getMessagesConfig().getMessageList("help.menu"))
            if (str.equalsIgnoreCase("%1"))
                for (String command : CommandHandler.commands.keySet())
                    event.getSender().sendMessageFormatted("help.menu-item", event.getLabel(), command, Triton.get()
                            .getMessagesConfig()
                            .getMessage("command." + command));
            else
                event.getSender().sendMessage(str);

        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
