package com.rexcantor64.triton.bungeecord.commands;

import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import lombok.val;
import net.md_5.bungee.api.connection.ProxiedPlayer;

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
            return BungeeTriton.asBungee().getLoader().getProxy().getPlayers().stream()
                    .map(ProxiedPlayer::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(partialName))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
