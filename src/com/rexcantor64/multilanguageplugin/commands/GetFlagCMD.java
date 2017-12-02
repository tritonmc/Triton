package com.rexcantor64.multilanguageplugin.commands;

import com.google.common.collect.Lists;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class GetFlagCMD implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage("Only Players.");
            return true;
        }

        Player p = (Player) s;

        if (!p.hasPermission("multilanguageplugin.getflag")) {
            p.sendMessage(SpigotMLP.get().getMessage("error.no-permission"));
            return true;
        }

        if (args.length == 1) {
            p.sendMessage(SpigotMLP.get().getMessage("help.getflag", label));
            return true;
        }

        Language lang = SpigotMLP.get().getLanguageManager().getLanguageByName(args[1], false);
        if (lang == null) {
            p.sendMessage(SpigotMLP.get().getMessage("error.lang-not found", args[1]));
            return true;
        }

        p.getInventory().addItem(lang.getStack());
        p.sendMessage(SpigotMLP.get().getMessage("success.getflag", lang.getDisplayName()));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("multilanguageplugin.getflag"))
            return tab;
        if (args.length == 2)
            for (Language lang : SpigotMLP.get().getLanguageManager().getAllLanguages())
                if (lang.getName().startsWith(args[1]))
                    tab.add(lang.getName());
        return tab;
    }

}
