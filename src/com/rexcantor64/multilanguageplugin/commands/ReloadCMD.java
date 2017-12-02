package com.rexcantor64.multilanguageplugin.commands;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("multilanguageplugin.reload")) {
            s.sendMessage(SpigotMLP.get().getMessage("error.no-permission"));
            return true;
        }

        SpigotMLP.get().reload();
        s.sendMessage(SpigotMLP.get().getMessage("success.reload"));
        return true;
    }

}
