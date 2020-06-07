package com.rexcantor64.triton.commands.bungee;

import com.google.gson.JsonParser;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.web.TwinManager;
import com.rexcantor64.triton.web.TwinParser;
import lombok.val;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;

@SuppressWarnings("deprecation")
public class TwinCMD extends Command {

    private String lastDownload = "";

    public TwinCMD() {
        super("twin");

    }

    @Override
    public void execute(CommandSender s, String[] args) {
        if (args.length == 0) {
            if (!s.hasPermission("twin.upload")) {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "twin.upload"));
                return;
            }
            BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), () -> upload(s));
        } else {
            if (!s.hasPermission("twin.download")) {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "twin.download"));
                return;
            }
            BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), () -> download(s,
                    args[0]));
        }
    }

    private void upload(CommandSender s) {
        s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.connecting"));

        TwinManager.HttpResponse response = Triton.get().getTwinManager().upload();

        if (response == null) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.failed-bungeecord"));
            return;
        }

        if (response.getStatusCode() == 0) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.no-internet", response.getPage()));
            return;
        }

        if (response.getStatusCode() == 401) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.no-token"));
            return;
        }

        if (response.getStatusCode() != 200) {
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("twin.failed-upload", Triton.get().getMessagesConfig()
                            .getMessage("twin.incorrect-status",
                                    response.getStatusCode())));
            return;
        }

        s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.uploaded",
                "https://twin.rexcantor64.com/" + response.getPage()));
    }

    private void download(CommandSender s, String id) {
        if (id.equals(lastDownload)) {
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("twin.repeated-download"));
            lastDownload = "";
            return;
        }

        s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.connecting"));

        TwinManager.HttpResponse response = Triton.get().getTwinManager().download(id);

        if (response == null) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.failed-bungeecord"));
            return;
        }

        if (response.getStatusCode() == 0) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.no-internet", response.getPage()));
            return;
        }

        if (response.getStatusCode() != 200) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.failed-fetch",
                    Triton.get().getMessagesConfig().getMessage("twin.incorrect-status",
                            response.getStatusCode())));
            return;
        }

        lastDownload = id;

        Triton.get().runAsync(() -> {
            try {
                val storage = Triton.get().getStorage();

                long start = System.currentTimeMillis();
                Triton.get().getLogger().logInfo(2, "Parsing changes from TWIN...");
                val data = new JsonParser().parse(response.getPage()).getAsJsonObject();
                val collections = TwinParser.parseDownload(storage.getCollections(), data);

                Triton.get().getLogger().logInfo(2, "Saving changes to permanent storage...");
                storage.setCollections(collections);
                storage.uploadToStorage(collections);

                Triton.get().getLogger().logInfo(2, "Reloading translation manager...");
                Triton.get().getLanguageManager().setup();
                if (Triton.isBungee())
                    Triton.asBungee().getBridgeManager().sendConfigToEveryone();
                Triton.get().refreshPlayers();

                Triton.get().getLogger()
                        .logInfo(2, "[TWIN] Parsed and saved changes and restarted translation manager in %1 ms!",
                                System.currentTimeMillis() - start);

                s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.success"));
            } catch (Exception e) {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.failed-fetch", e.getMessage()));
            }
        });
    }
}
