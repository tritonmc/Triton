package com.rexcantor64.multilanguageplugin.commands;

import com.google.common.collect.Lists;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.Language;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class SetLanguageCMD implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!(s instanceof Player)) {
            s.sendMessage("Only Players.");
            return true;
        }

        Player p = (Player) s;

        if (!p.hasPermission("multilanguageplugin.setlanguage")) {
            p.sendMessage(SpigotMLP.get().getMessage("error.no-permission"));
            return true;
        }

        if (args.length == 1) {
            p.sendMessage(SpigotMLP.get().getMessage("help.setlanguage", label));
            return true;
        }

        Player target = p;
        String langName = args[1];

        if (args.length >= 3) {
            langName = args[2];
            if (p.hasPermission("multilanguageplugin.setlanguage.others")) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    p.sendMessage(SpigotMLP.get().getMessage("error.player-not-found", args[1]));
                    return true;
                }
            }
        }

        Language lang = SpigotMLP.get().getLanguageManager().getLanguageByName(langName, false);
        if (lang == null) {
            p.sendMessage(SpigotMLP.get().getMessage("error.lang-not-found", args[1]));
            return true;
        }

        SpigotMLP.get().getPlayerManager().get(target).setLang(lang);
        if (target == p)
            p.sendMessage(SpigotMLP.get().getMessage("success.setlanguage", lang.getDisplayName()));
        else
            p.sendMessage(SpigotMLP.get().getMessage("success.setlanguage-others", target.getName(),
                    lang.getDisplayName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("multilanguageplugin.setlanguage"))
            return tab;
        if (args.length == 2 || (args.length == 3) && s.hasPermission("multilanguageplugin.setlanguage.others"))
            for (Language lang : SpigotMLP.get().getLanguageManager().getAllLanguages())
                if (lang.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                    tab.add(lang.getName());
        return tab;
    }

}
