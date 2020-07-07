package com.rexcantor64.triton.commands;

import com.google.gson.JsonParser;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.web.TwinParser;
import lombok.val;

import java.util.Collections;
import java.util.List;

public class TwinCommand implements Command {

    private String lastDownload = "";

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        val downloading = event.getArgs().length > 0;

        if (downloading)
            sender.assertPermission("twin.download");
        else
            sender.assertPermission("twin.upload");

        if (downloading && event.getArgs()[0].equals(lastDownload)) {
            sender.sendMessageFormatted("twin.repeated-download");
            lastDownload = "";
            return true;
        }

        sender.sendMessageFormatted("twin.connecting");

        Triton.get().runAsync(() -> {
            val twinManager = Triton.get().getTwinManager();
            val response = downloading ? twinManager.download(event.getArgs()[0]) : twinManager.upload();

            if (response == null) {
                sender.sendMessageFormatted("twin.failed-bungeecord");
                return;
            }

            if (response.getStatusCode() == 0) {
                sender.sendMessageFormatted("twin.no-internet", response.getPage());
                return;
            }

            if (response.getStatusCode() == 401) {
                sender.sendMessageFormatted("twin.no-token");
                return;
            }

            if (response.getStatusCode() != 200) {
                sender.sendMessageFormatted(downloading ? "twin.failed-fetch" : "twin.failed-upload", Triton.get()
                        .getMessagesConfig().getMessage("twin.incorrect-status", response.getStatusCode()));
                return;
            }

            if (!downloading) {
                sender.sendMessageFormatted("twin.uploaded", "https://twin.rexcantor64.com/" + response.getPage());
                return;
            }

            lastDownload = event.getArgs()[0];

            handleDownload(event, response.getPage());

        });

        return true;
    }

    private void handleDownload(CommandEvent event, String response) {
        try {
            val storage = Triton.get().getStorage();

            long start = System.currentTimeMillis();
            Triton.get().getLogger().logInfo(2, "Parsing changes from TWIN...");
            val data = new JsonParser().parse(response).getAsJsonObject();
            val twinResponse = TwinParser.parseDownload(storage.getCollections(), data);

            Triton.get().getLogger().logInfo(2, "Saving changes to permanent storage...");
            storage.setCollections(twinResponse.getCollections());
            storage.uploadPartiallyToStorage(twinResponse.getCollections(), twinResponse.getChanged(), twinResponse
                    .getDeleted());

            Triton.get().getLogger().logInfo(2, "Reloading translation manager...");
            Triton.get().getLanguageManager().setup();
            if (Triton.isBungee())
                Triton.asBungee().getBridgeManager().sendConfigToEveryone();
            Triton.get().refreshPlayers();

            Triton.get().getLogger()
                    .logInfo(2, "[TWIN] Parsed and saved changes and restarted translation manager in %1 ms!",
                            System.currentTimeMillis() - start);

            event.getSender().sendMessageFormatted("twin.success");
        } catch (Exception e) {
            event.getSender().sendMessageFormatted("twin.failed-fetch", e.getMessage());
            if (Triton.get().getConfig().getLogLevel() > 0)
                e.printStackTrace();
        }
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
