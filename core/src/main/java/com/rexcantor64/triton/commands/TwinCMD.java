package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.web.TwinManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TwinCMD implements CommandExecutor {

    private String lastDownload = "";

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if (!s.hasPermission("twin.upload")) {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "twin.upload"));
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(Triton.get().getLoader().asSpigot(), () -> upload(s));
        } else {
            if (!s.hasPermission("twin.download")) {
                s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "twin.download"));
                return true;
            }
            Bukkit.getScheduler().runTaskAsynchronously(Triton.get().getLoader().asSpigot(), () -> download(s,
                    args[0]));
        }
        return true;
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
                            .getMessage("twin.incorrect-status", response.getStatusCode())));
            return;
        }

        s.sendMessage(Triton.get().getMessagesConfig()
                .getMessage("twin.uploaded", "https://twin.rexcantor64.com/" + response.getPage()));
    }

    private void download(CommandSender s, String id) {
        if (id.equals(lastDownload)) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("twin.repeated-download"));
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
            s.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("twin.failed-fetch", Triton.get().getMessagesConfig()
                            .getMessage("twin.incorrect-status", response.getStatusCode())));
            return;
        }

        lastDownload = id;

        s.sendMessage("[Triton v3 BETA] This has been disabled for now due to changes in the source code.");

        /*try {
            JSONObject responseJson = new JSONObject(response.getPage());
            JSONArray storage = Triton.get().getLanguageConfig().getRaw();
            JSONArray deleted = responseJson.optJSONArray("deleted");
            JSONObject modified = responseJson.optJSONObject("modified");

            storageLoop:
            for (int k = 0; k < storage.length(); k++) {
                JSONObject obj = storage.optJSONObject(k);
                if (obj == null) continue;
                JSONObject twin = obj.optJSONObject("_twin");
                if (twin == null) continue;

                if (deleted != null)
                    for (int i = 0; i < deleted.length(); i++) {
                        String key = deleted.optString(i);
                        if (key.isEmpty()) continue;
                        if (key.equals(twin.optString("id"))) {
                            storage.remove(k--);
                            continue storageLoop;
                        }
                    }
                if (modified != null) {
                    String key = twin.optString("id");
                    if (!key.isEmpty() && modified.optJSONArray(key) != null)
                        JSONUtils.applyPatches(obj, modified.optJSONArray(key));
                }
            }
            JSONArray added = responseJson.optJSONArray("added");

            if (added != null)
                for (int k = 0; k < added.length(); k++)
                    if (added.optJSONObject(k) != null) storage.put(added.optJSONObject(k));

            try {
                Triton.get().getLanguageConfig().saveFromRaw(storage);
            } catch (Exception e) {
                s.sendMessage(Triton.get().getMessage("twin.failed-file-update", "&cError while writing to file '%1':" +
                        " %2", "languages.json", e.getMessage()));
            }
            Triton.get().reload();
            s.sendMessage(Triton.get().getMessage("twin.success", "&aSuccessfully fetched the config from TWIN and " +
                    "applied it into the server!"));
        } catch (Exception e) {
            s.sendMessage(Triton.get().getMessage("twin.failed-fetch", "&cFailed to fetch the config: %1",
                    e.getMessage()));
        }*/
    }
}
