package com.rexcantor64.triton.commands.handler;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommand extends Command implements TabExecutor {
    private final BungeeCommandHandler handler;


    public BungeeCommand(BungeeCommandHandler handler, String name, String... aliases) {
        super(name, null, aliases);
        this.handler = handler;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        this.handler.onCommand(getName(), sender, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return this.handler.onTabComplete(getName(), sender, args);
    }
}
