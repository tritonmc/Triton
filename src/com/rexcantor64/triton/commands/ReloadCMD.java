package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.MultiLanguagePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("multilanguageplugin.reload") && !s.hasPermission("triton.reload")) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.reload"));
            return true;
        }

        MultiLanguagePlugin.get().reload();
        s.sendMessage(MultiLanguagePlugin.get().getMessage("success.reload", "&aConfig successfully reloaded."));
        return true;
    }

}
