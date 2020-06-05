package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.val;
import net.md_5.bungee.api.CommandSender;

public class DatabaseCMD implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (!s.hasPermission("triton.database")) {
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("error.no-permission", "triton.database"));
            return;
        }

        if (args.length < 2) {
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("help.database", "triton"));
            return;
        }

        val storage = Triton.get().getStorage();
        if (storage instanceof LocalStorage) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.database-not-supported"));
            return;
        }

        val mode = args[1];
        if (mode.equalsIgnoreCase("upload") || mode.equalsIgnoreCase("u")) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("other.database-loading"));
            Triton.get().runAsync(() -> {
                val localStorage = new LocalStorage();
                val collections = localStorage.downloadFromStorage();

                Triton.get().getStorage().uploadToStorage(collections);
                Triton.get().getStorage().setCollections(collections);

                Triton.get().getLanguageManager().setup();
                Triton.asBungee().getBridgeManager().sendConfigToEveryone();
                Triton.get().refreshPlayers();

                s.sendMessage(Triton.get().getMessagesConfig().getMessage("success.database"));
            });
        } else if (mode.equals("download") || mode.equals("d")) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("other.database-loading"));
            Triton.get().runAsync(() -> {
                val localStorage = new LocalStorage();
                localStorage.uploadToStorage(Triton.get().getStorage().getCollections());

                s.sendMessage(Triton.get().getMessagesConfig().getMessage("success.database"));
            });
        } else {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.database-invalid-mode", mode));
        }
    }
}
