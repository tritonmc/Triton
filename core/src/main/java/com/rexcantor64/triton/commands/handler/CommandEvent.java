package com.rexcantor64.triton.commands.handler;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class CommandEvent {
    private final Sender sender;
    private final String subCommand;
    private final String[] args;
    private final String label;
    private final Environment environment;

    @RequiredArgsConstructor
    @Getter
    public enum Environment {
        SPIGOT(false), BUNGEE(true), VELOCITY(true);

        private final boolean proxy;
    }

}
