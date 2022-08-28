package com.rexcantor64.triton.spigot.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class OpenSelectorCommand extends com.rexcantor64.triton.commands.OpenSelectorCommand {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, UnsupportedPlatformException {
        super.handleCommand(event);
        val sender = event.getSender();
        val uuid = sender.getUUID();

        sender.assertPermission("triton.openselector", "multilanguageplugin.openselector");

        SpigotTriton.asSpigot().openLanguagesSelectionGUI(Triton.get().getPlayerManager().get(uuid));
    }

}
