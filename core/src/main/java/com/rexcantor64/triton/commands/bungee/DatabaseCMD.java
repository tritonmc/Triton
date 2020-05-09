package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import lombok.val;
import net.md_5.bungee.api.CommandSender;

public class DatabaseCMD implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (!s.hasPermission("triton.database")) {
            s.sendMessage(Triton.get()
                    .getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton" +
                            ".database"));
            return;
        }

        if (args.length < 2) {
            s.sendMessage(Triton.get()
                    .getMessage("help.database", "&cUse &4/%1 database <upload/download/u/d>", "triton"));
            return;
        }

        val mode = args[1];
        if (mode.equalsIgnoreCase("upload") || mode.equalsIgnoreCase("u")) {
            try {
                s.sendMessage(Triton.get().getMessage("other.database-loading", "&aPerforming operation..."));
                Triton.get().runAsync(() -> {
                    val metadata = Triton.get().getLanguageConfig().getMetadataList();
                    val items = Triton.get().getLanguageConfig().getRaw();
                    Triton.get().getStorage().uploadToStorage(metadata, items);
                    s.sendMessage(Triton.get().getMessage("success.database", "&aOperation successful"));
                });
            } catch (UnsupportedOperationException e) {
                s.sendMessage(Triton.get()
                        .getMessage("error.database-not-supported", "&cThis command isn't supported on local storage" +
                                "."));
            }
        } else if (mode.equals("download") || mode.equals("d")) {
            s.sendMessage("[Triton v3 BETA] Not implemented yet");
        } else {
            s.sendMessage(Triton.get()
                    .getMessage("error.database-invalid-mode", "&cMode &4%1&c does not exist. Available modes are " +
                            "'upload' (or 'u') and 'download' (or 'd').", mode));
        }
    }
}
