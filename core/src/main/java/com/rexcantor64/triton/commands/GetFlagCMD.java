package com.rexcantor64.triton.commands;

import com.google.common.collect.Lists;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.wrappers.items.ItemStackParser;
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

        if (!p.hasPermission("multilanguageplugin.getflag") && !p.hasPermission("triton.getflag")) {
            p.sendMessage(Triton.get().getMessagesConfig()
                    .getMessage("error.no-permission", "triton.getflag"));
            return true;
        }

        if (args.length == 1) {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("help.getflag", label));
            return true;
        }

        com.rexcantor64.triton.language.Language lang = Triton.get().getLanguageManager()
                .getLanguageByName(args[1], false);
        if (lang == null) {
            p.sendMessage(Triton.get().getMessagesConfig().getMessage("error.lang-not found", args[1]));
            return true;
        }

        p.getInventory().addItem(ItemStackParser.bannerToItemStack(lang.getBanner(), false));
        p.sendMessage(Triton.get().getMessagesConfig().getMessage("success.getflag", lang.getDisplayName()));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("multilanguageplugin.getflag") && !s.hasPermission("triton.getflag"))
            return tab;
        if (args.length == 2)
            for (Language lang : Triton.get().getLanguageManager().getAllLanguages())
                if (lang.getName().startsWith(args[1]))
                    tab.add(lang.getName());
        return tab;
    }

}
