package com.rexcantor64.triton.bungeecord.commands.handler;

import com.rexcantor64.triton.commands.handler.Command;
import net.md_5.bungee.api.CommandSender;

public class BungeeCommand extends Command {
    private BungeeCommandHandler handler;


    public BungeeCommand(BungeeCommandHandler handler, String name, String... aliases) {
        super(name, null, aliases);
        this.handler = handler;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.handler.onCommand(getName(), sender, args);
    }
}
