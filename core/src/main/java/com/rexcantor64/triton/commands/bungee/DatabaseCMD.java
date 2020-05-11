package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
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

        val mode = args[1];
        if (mode.equalsIgnoreCase("upload") || mode.equalsIgnoreCase("u")) {
            try {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("other.database-loading"));
                // TODO
                /*Triton.get().runAsync(() -> {
                    val metadata = Triton.get().getLanguageConfig().getMetadataList();
                    val items = Triton.get().getLanguageConfig().getRaw();
                    Triton.get().getStorage().uploadToStorage(metadata, items);
                    s.sendMessage(Triton.get().getMessagesConfig().getMessage("success.database"));
                });*/
            } catch (UnsupportedOperationException e) {
                s.sendMessage(Triton.get().getMessagesConfig()
                        .getMessage("error.database-not-supported"));
            }
        } else if (mode.equals("download") || mode.equals("d")) {
            s.sendMessage("[Triton v3 BETA] Not implemented yet");
        } else {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.database-invalid-mode", mode));
        }
    }
}
