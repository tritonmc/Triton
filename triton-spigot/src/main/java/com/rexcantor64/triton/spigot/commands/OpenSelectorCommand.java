package com.rexcantor64.triton.spigot.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;

public class OpenSelectorCommand extends com.rexcantor64.triton.commands.OpenSelectorCommand {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, PlayerOnlyCommandException, UnsupportedPlatformException {
        super.handleCommand(event);
        val sender = event.getSender();
        val uuid = sender.getUUID();

        sender.assertPermission("triton.openselector", "multilanguageplugin.openselector");

        SpigotTriton.asSpigot().openLanguagesSelectionGUI(Triton.get().getPlayerManager().get(uuid));
    }

}
