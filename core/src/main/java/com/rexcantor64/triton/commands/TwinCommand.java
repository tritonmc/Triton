package com.rexcantor64.triton.commands;

import com.google.gson.JsonParser;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.web.TwinManager;
import com.rexcantor64.triton.web.TwinParser;
import com.rexcantor64.triton.web.exceptions.NotOnProxyException;
import lombok.val;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TwinCommand implements Command {

    private String lastDownload = "";

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException {
        val sender = event.getSender();
        val args = event.getArgs();
        val downloading = args.length > 0 && !args[0].equalsIgnoreCase("upload");

        if (downloading) {
            sender.assertPermission("twin.download");
        } else {
            sender.assertPermission("twin.upload");
        }

        if (downloading && args[0].equals(lastDownload)) {
            sender.sendMessageFormatted("twin.repeated-download");
            lastDownload = "";
            return;
        }

        sender.sendMessageFormatted("twin.connecting");

        Triton.get().runAsync(() -> {
            val twinManager = Triton.get().getTwinManager();

            TwinManager.HttpResponse response;
            try {
                if (downloading) {
                    response = twinManager.download(args[0]);
                } else {
                    List<String> collections = null;
                    if (args.length >= 2 && !args[1].equalsIgnoreCase("*"))
                        collections = Arrays.asList(args[1].split(":"));

                    List<String> languages = null;
                    if (args.length >= 3 && !args[2].equalsIgnoreCase("*"))
                        languages = Arrays.asList(args[2].split(":"));

                    response = twinManager.upload(collections, languages);
                }
            } catch (NotOnProxyException exception) {
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
                if (response.getStatusCode() == 404) {
                    // Warn people that accidentally type "/twin reload" instead of "/triton reload" for example
                    sender.sendMessageFormatted("twin.suggestion-wrong-command", "/triton " + event.argumentsToString());

                }
                return;
            }

            if (!downloading) {
                sender.sendMessageFormatted("twin.uploaded", TwinManager.getBaseUrl() + "/" + response.getPage());
                return;
            }

            lastDownload = args[0];

            handleDownload(event, response.getPage());

        });
    }

    private void handleDownload(CommandEvent event, String response) {
        try {
            val storage = Triton.get().getStorage();

            long start = System.currentTimeMillis();
            Triton.get().getLogger().logDebug("[TWIN] Parsing changes from TWIN...");
            val data = JsonParser.parseString(response).getAsJsonObject();
            val twinResponse = TwinParser.parseDownload(storage.getCollections(), data);

            Triton.get().getLogger().logDebug("[TWIN] Saving changes to permanent storage...");
            storage.setCollections(twinResponse.getCollections());
            if (!storage.uploadPartiallyToStorage(twinResponse.getCollections(), twinResponse.getChanged(), twinResponse
                    .getDeleted())) {
                event.getSender().sendMessageFormatted("twin.failed-collection-save");
                return;
            }

            Triton.get().getLogger().logDebug("[TWIN] Reloading translation manager...");
            Triton.get().getLanguageManager().setup();
            Triton.get().getTranslationManager().setup();

            if (Triton.isProxy()) {
                Triton.get().getBridgeManager().sendConfigToEveryone();
            }

            Triton.get().refreshPlayers();

            Triton.get().getLogger()
                    .logDebug("[TWIN] Parsed and saved changes and restarted translation manager in %1 ms!",
                            System.currentTimeMillis() - start);

            event.getSender().sendMessageFormatted("twin.success");
        } catch (Exception e) {
            event.getSender().sendMessageFormatted("twin.failed-fetch", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
