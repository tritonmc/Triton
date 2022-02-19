package com.rexcantor64.triton.commands.handler;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.StringJoiner;

@Data
public class CommandEvent {
    private final Sender sender;
    private final String subCommand;
    private final String[] args;
    private final String label;
    private final Environment environment;

    /**
     * Join the sub command with the arguments,
     * in order to recreate the original sub command typed by the player.
     *
     * If the player typed "/triton setlanguage en_GB Player1",
     * this will return "setlanguage en_GB Player1"
     *
     * @return The sub command and arguments joined by a space.
     */
    public String getFullSubCommand() {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.setEmptyValue("");
        if (this.subCommand != null) {
            joiner.add(this.subCommand);
        }
        if (this.args != null) {
            Arrays.stream(this.args).forEach(joiner::add);
        }
        return joiner.toString();
    }

    @RequiredArgsConstructor
    @Getter
    public enum Environment {
        SPIGOT(false), BUNGEE(true), VELOCITY(true);

        private final boolean proxy;
    }

}
