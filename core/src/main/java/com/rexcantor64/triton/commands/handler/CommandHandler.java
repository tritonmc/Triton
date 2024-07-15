package com.rexcantor64.triton.commands.handler;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.*;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import lombok.val;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CommandHandler {

    protected final HashMap<String, Command> commands = new HashMap<>();

    public CommandHandler() {
        commands.put("database", new DatabaseCommand());
        commands.put("getflag", new GetFlagCommand());
        commands.put("help", new HelpCommand(this));
        commands.put("info", new InfoCommand());
        commands.put("loglevel", new LogLevelCommand());
        commands.put("openselector", new OpenSelectorCommand());
        commands.put("reload", new ReloadCommand());
        commands.put("setlanguage", new SetLanguageCommand());
        commands.put("sign", new SignCommand());
        commands.put("twin", new TwinCommand());
        commands.put("debug", new DebugCommand());
    }

    public void handleCommand(CommandEvent event) {
        if (event.getLabel().equalsIgnoreCase("twin")) {
            event = new CommandEvent(event.getSender(),
                    "twin",
                    event.getSubCommand() == null ?
                            new String[0] :
                            mergeSubcommandWithArgs(event.getSubCommand(), event.getArgs()),
                    "twin",
                    false
            );
        }

        try {
            // No args given
            Command command = commands.get(event.getSubCommand() == null ? "openselector" : event.getSubCommand());

            if (command == null) {
                command = commands.get("help");
            }

            command.handleCommand(event);
        } catch (PlayerOnlyCommandException e) {
            event.getSender().sendMessage("Only players");
        } catch (NoPermissionException e) {
            event.getSender().sendMessageFormatted("error.no-permission", e.getPermission());
        } catch (UnsupportedPlatformException e) {
            Triton.get().getBridgeManager().forwardCommand(event);
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "An unexpected exception happened while handling a command");
        }
    }

    protected List<String> handleTabCompletion(CommandEvent event) {
        if (event.getLabel().equalsIgnoreCase("twin")) {
            return Collections.emptyList();
        }

        val subCommand = event.getSubCommand();
        try {
            if (subCommand == null || event.getArgs().length == 0) {
                return commands.keySet().stream().filter(cmd -> subCommand != null && cmd.startsWith(subCommand))
                        .collect(Collectors.toList());
            }

            val command = commands.get(subCommand);
            if (command == null) {
                return Collections.emptyList();
            }

            return command.handleTabCompletion(event);
        } catch (NoPermissionException e) {
            return Collections.emptyList();
        }
    }

    private String[] mergeSubcommandWithArgs(String subCommand, String[] args) {
        val newLength = args.length + 1;
        val newArray = new String[newLength];
        newArray[0] = subCommand;
        System.arraycopy(args, 0, newArray, 1, args.length);
        return newArray;
    }

    public Collection<String> getAvailableCommands() {
        return Collections.unmodifiableCollection(this.commands.keySet());
    }

}
