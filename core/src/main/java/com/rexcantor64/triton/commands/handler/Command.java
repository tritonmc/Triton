package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public interface Command {

    void handleCommand(CommandEvent event) throws NoPermissionException, PlayerOnlyCommandException, UnsupportedPlatformException;

    List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException;

    default void assertPlayersOnly(CommandEvent event) throws PlayerOnlyCommandException {
        if (event.getSender().getUUID() == null) {
            throw new PlayerOnlyCommandException();
        }
    }

    default <T extends Enum<T>> Optional<T> getSubcommandFromName(T[] values, String name) {
        for (T subcommand : values) {
            if (subcommand.name().equalsIgnoreCase(name)) {
                return Optional.of(subcommand);
            }
        }
        return Optional.empty();
    }

    default <T extends Enum<T>> String getSubcommandList(T[] values) {
        StringJoiner joiner = new StringJoiner(", ");
        for (T subcommand : values) {
            joiner.add(subcommand.name().toLowerCase());
        }
        return joiner.toString();
    }

}
