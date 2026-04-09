package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class InfoCommand implements Command {
    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException {
        val sender = event.getSender();
        sender.assertPermission("triton.info");

        sender.sendMessageFormatted(
                "info-command",
                Triton.get().getVersion(),
                "Rexcantor64 (Diogo Correia)",
                Triton.get().getStorage().toString(),
                event.getPlatform().isProxy() || Triton.get().getConfig().isBungeecord()
        );
        Triton.get().getLogger().logTrace("Current config: %1", Triton.get().getConfig());
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }

}
