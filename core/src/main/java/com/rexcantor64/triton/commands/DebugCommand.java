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
                // TODO get from messages.yml
                sendMessage(event, "You must provide a target platform: " + getSubcommandList(TargetPlatform.values()));
                return;
            }

            val targetPlatform = getSubcommandFromName(TargetPlatform.values(), args[0]);
            if (!targetPlatform.isPresent()) {
                // TODO get from messages.yml
                sendMessage(event,"Invalid target platform. Available: " + getSubcommandList(Subcommand.values()));
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
            // TODO get from messages.yml
            sendMessage(event,"You must provide a subcommand: " + getSubcommandList(Subcommand.values()));
            return;
        }

        val subcommand = getSubcommandFromName(Subcommand.values(), args[0]);
        if (!subcommand.isPresent()) {
            // TODO get from messages.yml
            sendMessage(event, "Invalid subcommand. Available: " + getSubcommandList(Subcommand.values()));
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
            // TODO get from messages.yml
            sendMessage(event, "You must provide a dump subcommand: " + getSubcommandList(DumpSubcommand.values()));
            return;
        }

        val subcommand = getSubcommandFromName(DumpSubcommand.values(), args[1]);
        if (!subcommand.isPresent()) {
            // TODO get from messages.yml
            sendMessage(event, "Invalid dump subcommand. Available: " + getSubcommandList(DumpSubcommand.values()));
            return;
        }

        val dumpManager = Triton.get().getDumpManager();
        switch (subcommand.get()) {
            case ADD:
            case REMOVE:
                if (args.length < 3) {
                    // TODO get from messages.yml
                    sendMessage(event, "You must provide a player, 'me' or 'all'");
                    return;
                }
                val playerStr = args[2];
                List<FeatureSyntax> types = new ArrayList<>();
                if (args.length >= 4) {
                    for (int i = 3; i < args.length; ++i) {
                        val type = dumpManager.getAvailableTypes().get(args[i]);
                        if (type == null) {
                            // TODO get from messages.yml
                            sendMessage(event,"Type " + args[i] + " not found");
                            return;
                        }
                        types.add(type);
                    }
                } else {
                    types.addAll(dumpManager.getAvailableTypes().values());
                }

                if (playerStr.equalsIgnoreCase("all")) {
                    if (subcommand.get() == DumpSubcommand.ADD) {
                        dumpManager.enableForEveryone(types);
                    } else {
                        dumpManager.disableForEveryone(types);
                    }
                } else {
                    UUID player;
                    if (playerStr.equalsIgnoreCase("me")) {
                        if (sender.getUUID() == null) {
                            sendMessage(event, "Only players can use 'me'");
                            return;
                        }
                        player = sender.getUUID();
                    } else {
                        val uuid = Triton.get().getPlayerUUIDFromString(playerStr);
                        if (uuid == null) {
                            // TODO get from messages.yml
                            sendMessage(event, "Can't find player " + playerStr);
                        }
                        player = uuid;
                    }
                    if (subcommand.get() == DumpSubcommand.ADD) {
                        dumpManager.enableForPlayer(player, types);
                    } else {
                        dumpManager.disableForPlayer(player, types);
                    }
                }
                // TODO get from messages.yml
                sendMessage(event, "Success");
                break;
            case CLEAR:
                dumpManager.disable();
                sendMessage(event, "Disabled dumping for everyone");
                break;
        }
    }

    public void handleLoadCommand(CommandEvent event) {
        val sender = event.getSender();
        val args = event.getArgs();

        if (args.length < 2) {
            // TODO get from messages.yml
            sendMessage(event, "You must provide a dump name");
            return;
        }

        val dumpName = args[1];

        int startLine = 0;
        int endLine = Integer.MAX_VALUE;

        try {
            if (args.length >= 3) {
                startLine = Integer.parseInt(args[2]);
                endLine = startLine + 1;
            }
            if (args.length >= 4) {
                endLine = Integer.parseInt(args[3]);
            }
        } catch (NumberFormatException e) {
            // TODO get from messages.yml
            sendMessage(event, "Invalid number");
            return;
        }

        try {
            val messages = LoadDump.getMessagesFromDump(dumpName, startLine, endLine);

            for (val message : messages) {
                sender.sendMessage(message);
            }

            // TODO get from messages.yml
            sendMessage(event, "Sent " + messages.size() + " messages!");
        } catch (IOException e) {
            // TODO get from messages.yml
            sendMessage(event, "Failed to open dump file: " + e.getMessage());
            Triton.get().getLogger().logError(e, "Failed to open dump file");
        } catch (JsonParseException e) {
            // TODO get from messages.yml
            sendMessage(event, "Invalid message JSON " + e.getMessage());
            Triton.get().getLogger().logError(e, "Failed to parse message JSON while loading dump");
        }
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) throws NoPermissionException {
        event.getSender().assertPermission("triton.debug");

        return Collections.emptyList();
    }

    /**
     * Wrapper to include platform in the message sent
     *
     * @param event   The command event being handled
     * @param message The message to send
     */
    private void sendMessage(CommandEvent event, String message) {
        event.getSender().sendMessage("[Triton @ " + event.getPlatform() + "] " + message);
    }

    private enum TargetPlatform {
        SERVER,
        PROXY,
    }

    private enum Subcommand {
        DUMP,
        LOAD
    }

    private enum DumpSubcommand {
        ADD,
        REMOVE,
        CLEAR
    }

}
