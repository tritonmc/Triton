package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class OpenSelectorCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        val uuid = sender.getUUID();

        if (event.getEnvironment() != CommandEvent.Environment.SPIGOT && uuid != null)
            return false;

        sender.assertPermission("triton.openselector", "multilanguageplugin.openselector");

        if (uuid == null)
            sender.sendMessage("Only players");
        else
            Triton.asSpigot().openLanguagesSelectionGUI(Triton.get().getPlayerManager().get(uuid));

        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
