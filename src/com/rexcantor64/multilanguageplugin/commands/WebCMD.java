package com.rexcantor64.multilanguageplugin.commands;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.web.GistUploader;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;

public class WebCMD implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        GistUploader.HttpResponse response = SpigotMLP.get().getGistUploader().upload();
        if(!response.isSuccess()){
            s.sendMessage("Failed! " + response.getPage());
            return true;
        }
        JSONObject r = new JSONObject(response.getPage());
        s.sendMessage("Gist URL: " + r.getString("html_url"));
        return true;
    }
}
