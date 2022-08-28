package com.rexcantor64.triton.commands.handler;

import lombok.val;
import net.md_5.bungee.api.CommandSender;

import java.util.Arrays;

public class BungeeCommandHandler extends CommandHandler {

    void onCommand(String label, CommandSender sender, String[] args) {
        val commandEvent = this.buildCommandEvent(label, sender, args);
        super.handleCommand(commandEvent);
    }

    Iterable<String> onTabComplete(String label, CommandSender sender, String[] args) {
        val commandEvent = this.buildCommandEvent(label, sender, args);
        return super.handleTabCompletion(commandEvent);
    }

    private CommandEvent buildCommandEvent(String label, CommandSender sender, String[] args) {
        val subCommand = args.length >= 1 ? args[0] : null;
        val subArgs = args.length >= 2 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

        return new CommandEvent(
                new BungeeSender(sender),
                subCommand,
                subArgs,
                label,
                CommandEvent.Environment.BUNGEE
        );
    }
}
