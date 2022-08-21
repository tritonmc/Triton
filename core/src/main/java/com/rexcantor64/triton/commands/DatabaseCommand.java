package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.val;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();

        sender.assertPermission("triton.database");

        if (event.getPlatform() == Platform.SPIGOT && Triton.get().getConfig().isBungeecord()) {
            sender.sendMessageFormatted("error.not-available-on-spigot");
            return true;
        }

        val args = event.getArgs();
        if (args.length == 0) {
            sender.sendMessageFormatted("help.database", event.getLabel());
            return true;
        }

        val storage = Triton.get().getStorage();
        if (storage instanceof LocalStorage) {
            sender.sendMessageFormatted("error.database-not-supported");
            return true;
        }

        val mode = args[0];
        switch (mode.toLowerCase()) {
            case "upload":
            case "u":
                sender.sendMessageFormatted("other.database-loading");
                Triton.get().runAsync(() -> {
                    val localStorage = new LocalStorage();
                    val collections = localStorage.downloadFromStorage();

                    Triton.get().getStorage().uploadToStorage(collections);
                    Triton.get().getStorage().setCollections(collections);

                    Triton.get().getLanguageManager().setup();
                    if (Triton.isProxy()) {
                        Triton.get().getBridgeManager().sendConfigToEveryone();
                    }
                    Triton.get().refreshPlayers();

                    sender.sendMessageFormatted("success.database");
                });
                break;
            case "download":
            case "d":
                sender.sendMessageFormatted("other.database-loading");
                Triton.get().runAsync(() -> {
                    val localStorage = new LocalStorage();
                    localStorage.uploadToStorage(Triton.get().getStorage().getCollections());

                    sender.sendMessageFormatted("success.database");
                });
                break;
            default:
                sender.sendMessageFormatted("error.database-invalid-mode", mode);
                break;
        }
        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        if (event.getArgs().length > 1)
            return Collections.emptyList();
        return Stream.of("download", "upload")
                .filter(v -> v.toLowerCase().startsWith(event.getArgs()[0].toLowerCase()))
                .collect(Collectors.toList());
    }
}
