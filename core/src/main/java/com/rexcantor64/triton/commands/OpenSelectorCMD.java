package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OpenSelectorCMD implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage("Only Players.");
            return true;
        }

        Player p = (Player) s;

        if (!p.hasPermission("multilanguageplugin.openselector") && !p.hasPermission("triton.openselector")) {
            p.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("error.no-permission", "triton.openselector"));
            return true;
        }

        Triton.get().openLanguagesSelectionGUI(Triton.get().getPlayerManager().get(p.getUniqueId()));
        return true;
    }

}
