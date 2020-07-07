package com.rexcantor64.triton.commands.handler;

import lombok.Data;

@Data
public class CommandEvent {
    private final Sender sender;
    private final String subCommand;
    private final String[] args;
    private final String label;
    private final Environment environment;

    public enum Environment {
        SPIGOT, BUNGEE;
    }

}
