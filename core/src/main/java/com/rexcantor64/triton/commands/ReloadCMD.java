package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("multilanguageplugin.reload") && !s.hasPermission("triton.reload")) {
            s.sendMessage(Triton.get().getMessagesConfig().getMessage("error.no-permission", "triton.reload"));
            return true;
        }

        Triton.get().reload();
        s.sendMessage(Triton.get().getMessagesConfig().getMessage("success.reload"));
        return true;
    }

}
