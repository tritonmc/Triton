package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class InfoCommand implements Command {
    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        sender.assertPermission("triton.info");

        Triton.get().getMessagesConfig().getMessageList("info-command").forEach((msg) -> sender
                .sendMessage(handleArguments(msg,
                        Triton.get().getVersion(),
                        "Rexcantor64 (Diogo Correia)",
                        Triton.get().getStorage().getClass().getSimpleName(),
                        Triton.isBungee() || Triton.get().getConfig().isBungeecord()
                )));

        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }

    private String handleArguments(String s, Object... args) {
        for (int i = 0; i < args.length; i++)
            if (args[i] != null)
                s = s.replace("%" + (i + 1), args[i].toString());
        return s;
    }
}
