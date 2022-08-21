package com.rexcantor64.triton.velocity.commands.handler;

import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.CommandHandler;
import com.rexcantor64.triton.plugin.Platform;
import com.velocitypowered.api.command.SimpleCommand;
import lombok.val;

import java.util.Arrays;
import java.util.List;

public class VelocityCommandHandler extends CommandHandler implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        super.handleCommand(buildCommandEvent(invocation, null));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        val a = super.handleTabCompletion(buildCommandEvent(invocation, ""));
        return a;
    }

    private CommandEvent buildCommandEvent(Invocation invocation, String defaltSubcommand) {
        val args = invocation.arguments();
        val subCommand = args.length >= 1 ? args[0] : defaltSubcommand;
        val subArgs = args.length >= 2 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
        return new CommandEvent(
                new VelocitySender(invocation.source()),
                subCommand,
                subArgs,
                invocation.alias(),
                Platform.VELOCITY
        );
    }
}
