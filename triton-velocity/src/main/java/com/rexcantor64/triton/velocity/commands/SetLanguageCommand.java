package com.rexcantor64.triton.velocity.commands;

import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.velocitypowered.api.proxy.Player;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SetLanguageCommand extends com.rexcantor64.triton.commands.SetLanguageCommand {

    @Override
    public List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException {
        val superResult = super.handleTabCompletion(event);
        if (!superResult.isEmpty()) {
            return superResult;
        }

        if (event.getArgs().length == 2) {
            val partialName = event.getArgs()[1].toLowerCase(Locale.ROOT);
            return VelocityTriton.asVelocity().getLoader().getServer().getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partialName))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
