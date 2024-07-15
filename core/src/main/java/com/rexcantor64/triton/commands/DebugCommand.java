package com.rexcantor64.triton.commands;

import com.google.gson.JsonParseException;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.debug.LoadDump;
import lombok.val;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DebugCommand implements Command {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, UnsupportedPlatformException {
        val sender = event.getSender();
        sender.assertPermission("triton.debug");

        String[] args = event.getArgs();

        if (event.isForwarded()) {
            // shift arguments for remaining code
            args = Arrays.copyOfRange(args, 1, args.length);
        } else if (sender.getUUID() != null && event.getPlatform().isProxy()) {
            if (args.length < 1) {
                sendMessage(event, "debug.target-platform.missing", getSubcommandList(TargetPlatform.values()));
                return;
            }

            val targetPlatform = getSubcommandFromName(TargetPlatform.values(), args[0]);
            if (!targetPlatform.isPresent()) {
                sendMessage(event, "debug.target-platform.invalid", args[0], getSubcommandList(TargetPlatform.values()));
                return;
            }

            if (targetPlatform.get() == TargetPlatform.SERVER) {
                Triton.get().getBridgeManager().forwardCommand(event);
                return;
            }

            // shift arguments for remaining code
            args = Arrays.copyOfRange(args, 1, args.length);
        }
        event = event.toBuilder().args(args).build();

        if (args.length < 1) {
            sendMessage(event, "debug.subcommand.missing", getSubcommandList(Subcommand.values()));
            return;
        }

        val subcommand = getSubcommandFromName(Subcommand.values(), args[0]);
        if (!subcommand.isPresent()) {
            sendMessage(event, "debug.subcommand.invalid", args[0], getSubcommandList(Subcommand.values()));
            return;
        }

        switch (subcommand.get()) {
            case DUMP:
                handleDumpCommand(event);
                break;
            case LOAD:
                handleLoadCommand(event);
                break;
        }
    }


    public void handleDumpCommand(CommandEvent event) {
        val sender = event.getSender();
        val args = event.getArgs();

        if (args.length < 2) {
            sendMessage(event, "debug.dump.action.missing", getSubcommandList(DumpAction.values()));
            return;
        }

        val action = getSubcommandFromName(DumpAction.values(), args[1]);
        if (!action.isPresent()) {
            sendMessage(event, "debug.dump.action.invalid", args[1], getSubcommandList(DumpAction.values()));
            return;
        }

        val dumpManager = Triton.get().getDumpManager();
        switch (action.get()) {
            case ADD:
            case REMOVE:
                if (args.length < 3) {
                    sendMessage(event, "debug.dump.player.missing");
                    return;
                }
                val playerStr = args[2];
                List<FeatureSyntax> types = new ArrayList<>();
                List<String> typeNames = new ArrayList<>();
                if (args.length >= 4) {
                    for (int i = 3; i < args.length; ++i) {
                        val typeName = args[i].toLowerCase();
                        val type = dumpManager.getAvailableTypes().get(typeName);
                        if (type == null) {
                            String allTypes = String.join(", ", dumpManager.getAvailableTypes().keySet());
                            sendMessage(event, "debug.dump.type.not-found", args[i], allTypes);
                            return;
                        }
                        types.add(type);
                        typeNames.add(typeName);
                    }
                } else {
                    types.addAll(dumpManager.getAvailableTypes().values());
                    typeNames.addAll(dumpManager.getAvailableTypes().keySet());
                }

                val typeNamesStr = String.join(", ", typeNames);

                if (playerStr.equalsIgnoreCase("all")) {
                    if (action.get() == DumpAction.ADD) {
                        dumpManager.enableForEveryone(types);
                        sendMessage(event, "debug.dump.success.add.all", typeNamesStr);
                    } else {
                        dumpManager.disableForEveryone(types);
                        sendMessage(event, "debug.dump.success.remove.all", typeNamesStr);
                    }
                } else {
                    UUID player;
                    if (playerStr.equalsIgnoreCase("me")) {
                        if (sender.getUUID() == null) {
                            sendMessage(event, "debug.dump.player.me-not-player");
                            return;
                        }
                        player = sender.getUUID();
                    } else {
                        val uuid = Triton.get().getPlayerUUIDFromString(playerStr);
                        if (uuid == null) {
                            sendMessage(event, "debug.dump.player.not-found", playerStr);
                            return;
                        }
                        player = uuid;
                    }
                    if (action.get() == DumpAction.ADD) {
                        dumpManager.enableForPlayer(player, types);
                        sendMessage(event, "debug.dump.success.add.player", typeNamesStr, player);
                    } else {
                        dumpManager.disableForPlayer(player, types);
                        sendMessage(event, "debug.dump.success.remove.player", typeNamesStr, player);
                    }
                }
                break;
            case CLEAR:
                dumpManager.disable();
                sendMessage(event, "debug.dump.clear");
                break;
        }
    }

    public void handleLoadCommand(CommandEvent event) {
        val sender = event.getSender();
        val args = event.getArgs();

        if (args.length < 2) {
            sendMessage(event, "debug.load.dump-file.missing");
            return;
        }

        val dumpName = args[1];

        int startLine = 0;
        int endLine = Integer.MAX_VALUE;

        if (args.length >= 3) {
            try {
                startLine = Integer.parseInt(args[2]);
                endLine = startLine + 1;
            } catch (NumberFormatException e) {
                sendMessage(event, "debug.load.line-number.invalid", args[2]);
                return;
            }
        }
        if (args.length >= 4) {
            try {
                endLine = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sendMessage(event, "debug.load.line-number.invalid", args[3]);
                return;
            }
        }

        try {
            val messages = LoadDump.getMessagesFromDump(dumpName, startLine, endLine);

            for (val message : messages) {
                sender.sendMessage(message);
            }

            sendMessage(event, "debug.load.success.sent", messages.size());
        } catch (IOException e) {
            sendMessage(event, "debug.load.dump-file.not-found", dumpName, e.getMessage());
            Triton.get().getLogger().logError(e, "Failed to open dump file");
        } catch (JsonParseException e) {
            sendMessage(event, "debug.load.dump-file.invalid-json", e.getMessage());
            Triton.get().getLogger().logError(e, "Failed to parse message JSON while loading dump");
        }
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException {
        val sender = event.getSender();
        String[] args = event.getArgs();

        sender.assertPermission("triton.debug");

        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (sender.getUUID() != null && event.getPlatform().isProxy()) {
            if (args.length == 1) {
                return autocompleteEnum(args[0], TargetPlatform.values());
            }
            args = Arrays.copyOfRange(args, 1, args.length);
        }

        if (args.length == 1) {
            return autocompleteEnum(args[0], Subcommand.values());
        }

        val subcommand = getSubcommandFromName(Subcommand.values(), args[0]);
        if (!subcommand.isPresent()) {
            return Collections.emptyList();
        }

        switch (subcommand.get()) {
            case DUMP:
                if (args.length == 2) {
                    return autocompleteEnum(args[1], DumpAction.values());
                }
                val dumpSubcommand = getSubcommandFromName(DumpAction.values(), args[1]);
                if (!dumpSubcommand.isPresent()) {
                    return Collections.emptyList();
                }

                switch (dumpSubcommand.get()) {
                    case ADD:
                    case REMOVE:
                        if (args.length == 3) {
                            val argLower = args[2].toLowerCase();
                            return Stream.of("all", "me")
                                    .filter(value -> value.startsWith(argLower))
                                    .collect(Collectors.toList());
                        }

                        val argLower = args[args.length - 1].toLowerCase();
                        return Triton.get().getDumpManager().getAvailableTypes().keySet().stream()
                                .map(String::toLowerCase)
                                .filter(value -> value.startsWith(argLower))
                                .collect(Collectors.toList());
                    case CLEAR:
                        break;
                }
                break;
            case LOAD:
                break;
        }

        return Collections.emptyList();
    }

    private <T extends Enum<T>> List<String> autocompleteEnum(String arg, T[] enumValues) {
        val argLower = arg.toLowerCase();
        return Arrays.stream(enumValues)
                .map(Enum::name)
                .map(String::toLowerCase)
                .filter(value -> value.startsWith(argLower))
                .collect(Collectors.toList());
    }

    /**
     * Wrapper to include platform in the message sent
     *
     * @param event The command event being handled
     * @param code  The code of the message to send
     * @param args  The arguments of the message
     */
    private void sendMessage(CommandEvent event, String code, Object... args) {
        event.getSender().sendMessageFormatted(
                "debug.prefix",
                event.getPlatform(),
                Triton.get().getMessagesConfig().getMessage(code, args)
        );
    }

    private enum TargetPlatform {
        SERVER,
        PROXY,
    }

    private enum Subcommand {
        DUMP,
        LOAD
    }

    private enum DumpAction {
        ADD,
        REMOVE,
        CLEAR
    }

}
