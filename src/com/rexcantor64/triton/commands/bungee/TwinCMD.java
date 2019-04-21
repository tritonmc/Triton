package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.JSONUtils;
import com.rexcantor64.triton.web.TwinManager;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

@SuppressWarnings("deprecation")
public class TwinCMD extends Command {

    public TwinCMD() {
        super("twin");

    }

    @Override
    public void execute(CommandSender s, String[] args) {
        if (args.length == 0) {
            if (!s.hasPermission("twin.upload")) {
                s.sendMessage(Triton.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "twin.upload"));
                return;
            }
            BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), () -> upload(s));
        } else {
            if (!s.hasPermission("twin.download")) {
                s.sendMessage(Triton.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "twin.download"));
                return;
            }
            BungeeCord.getInstance().getScheduler().runAsync(Triton.get().getLoader().asBungee(), () -> download(s, args[0]));
        }
    }

    private void upload(CommandSender s) {
        s.sendMessage(Triton.get().getMessage("twin.connecting", "&aConnecting to TWIN... Please wait"));

        TwinManager.HttpResponse response = Triton.get().getTwinManager().upload();

        if (response == null) {
            s.sendMessage(Triton.get().getMessage("twin.failed-bungeecord", "&cCan't upload the config because you have BungeeCord enabled on config! Please execute this command through BungeeCord."));
            return;
        }

        if (response.getStatusCode() == 0) {
            s.sendMessage(Triton.get().getMessage("twin.no-internet", "&4Failed to upload config. Please check your internet connection and/or firewall! Error description: %1", response.getPage()));
            return;
        }

        if (response.getStatusCode() == 401) {
            s.sendMessage(Triton.get().getMessage("twin.no-token", "&4Invalid token! Please check if you have setup TWIN correctly on config."));
            return;
        }

        if (response.getStatusCode() != 200) {
            s.sendMessage(Triton.get().getMessage("twin.failed-upload", "&cFailed to upload the config: %1", Triton.get().getMessage("twin.incorrect-status", "&4status is not 200 (received &l%1&4)", response.getStatusCode())));
            return;
        }

        s.sendMessage(Triton.get().getMessage("twin.uploaded", "&aYour config is live! Start editing now at &6%1", "https://twin.rexcantor64.com/" + response.getPage()));
    }

    private void download(CommandSender s, String id) {
        s.sendMessage(Triton.get().getMessage("twin.connecting", "&aConnecting to TWIN... Please wait"));

        TwinManager.HttpResponse response = Triton.get().getTwinManager().download(id);

        if (response == null) {
            s.sendMessage(Triton.get().getMessage("twin.failed-bungeecord", "&cCan't upload the config because you have BungeeCord enabled on config! Please execute this command through BungeeCord."));
            return;
        }

        if (response.getStatusCode() == 0) {
            s.sendMessage(Triton.get().getMessage("twin.no-internet", "&4please check your internet connection and/or firewall! Error description: %1", response.getPage()));
            return;
        }

        if (response.getStatusCode() != 200) {
            s.sendMessage(Triton.get().getMessage("twin.failed-fetch", "&cFailed to fetch the config: %1", Triton.get().getMessage("twin.incorrect-status", "&4status is not 200 (received &l%1&4)", response.getStatusCode())));
            return;
        }

        try {
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
                FileWriter fileWriter = new FileWriter(new File(Triton.get().getDataFolder(), "languages.json"));
                fileWriter.write(storage.toString(4));
                fileWriter.flush();
            } catch (Exception e) {
                s.sendMessage(Triton.get().getMessage("twin.failed-file-update", "&cError while writing to file '%1': %2", "languages.json", e.getMessage()));
            }
            Triton.get().reload();
            s.sendMessage(Triton.get().getMessage("twin.success", "&aSuccessfully fetched the config from TWIN and applied it into the server!"));
        } catch (Exception e) {
            s.sendMessage(Triton.get().getMessage("twin.failed-fetch", "&cFailed to fetch the config: %1", e.getMessage()));
        }
    }
}
