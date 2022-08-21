package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.plugin.Platform;
import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class SpigotCommandHandler extends CommandHandler implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        val subCommand = strings.length >= 1 ? strings[0] : null;
        val args = strings.length >= 2 ? Arrays.copyOfRange(strings, 1, strings.length) : new String[0];
        super.handleCommand(
                new CommandEvent(new SpigotSender(commandSender), subCommand, args, s)
        );
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] strings) {
        val subCommand = strings.length >= 1 ? strings[0] : null;
        val args = strings.length >= 2 ? Arrays.copyOfRange(strings, 1, strings.length) : new String[0];
        return super.handleTabCompletion(
                new CommandEvent(new SpigotSender(commandSender), subCommand, args, s)
        );
    }
}
