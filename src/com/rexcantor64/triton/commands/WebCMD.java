package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.web.GistManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;

public class WebCMD implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        try {
            s.sendMessage("Uploading...");
            GistManager.HttpResponse response = MultiLanguagePlugin.get().getGistManager().upload();
            if (!response.isSuccess() || response.getStatusCode() != 201) {
                s.sendMessage("Failed! " + response.getPage());
                return true;
            }
            JSONObject r = new JSONObject(response.getPage());
            s.sendMessage("Editor URL: https://mlpweb.rexcantor64.com/?id=" + r.optString("id", "Failed to upload!"));
        } catch (Exception e) {
            s.sendMessage("Failed! " + e.getMessage());
        }
        return true;
    }
}
