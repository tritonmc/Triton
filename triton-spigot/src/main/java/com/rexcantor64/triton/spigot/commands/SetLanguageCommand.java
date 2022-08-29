package com.rexcantor64.triton.spigot.commands;

import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class SetLanguageCommand extends com.rexcantor64.triton.commands.SetLanguageCommand {

    @Override
    public List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException {
        val superResult = super.handleTabCompletion(event);
        if (!superResult.isEmpty()) {
            return superResult;
        }

        if (event.getArgs().length == 2) {
            // returning "null" triggers the player list
            return null;
        }

        return Collections.emptyList();
    }
}
