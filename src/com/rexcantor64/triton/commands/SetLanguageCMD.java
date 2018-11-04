package com.rexcantor64.triton.commands;

import com.google.common.collect.Lists;
import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.Language;
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
        if (!s.hasPermission("multilanguageplugin.setlanguage") && !s.hasPermission("triton.setlanguage")) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.setlanguage"));
            return true;
        }

        if (args.length == 1) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("help.setlanguage", "&cUse /%1 setlanguage [player] <language name>", label));
            return true;
        }

        Player target;
        String langName;

        if (args.length >= 3) {
            langName = args[2];
            if (s.hasPermission("multilanguageplugin.setlanguage.others") || s.hasPermission("triton.setlanguage.others")) {
                target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    s.sendMessage(MultiLanguagePlugin.get().getMessage("error.player-not-found", "&cPlayer %1 not found!", args[1]));
                    return true;
                }
            } else {
                s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.setlanguage.others"));
                return true;
            }
        } else if (s instanceof Player) {
            target = (Player) s;
            langName = args[1];
        } else {
            s.sendMessage("Only Players.");
            return true;
        }

        Language lang = MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(langName, false);
        if (lang == null) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.lang-not-found", "&cLanguage %1 not found! Note: It's case sensitive. Use TAB to show all the available languages.", args[1]));
            return true;
        }

        MultiLanguagePlugin.get().getPlayerManager().get(target.getUniqueId()).setLang(lang);
        if (target == s)
            s.sendMessage(MultiLanguagePlugin.get().getMessage("success.setlanguage", "&aYour language has been changed to %1", lang.getDisplayName()));
        else
            s.sendMessage(MultiLanguagePlugin.get().getMessage("success.setlanguage-others", "&a%1's language has been changed to %2", target.getName(),
                    lang.getDisplayName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
        List<String> tab = Lists.newArrayList();
        if (!s.hasPermission("multilanguageplugin.setlanguage") && !s.hasPermission("triton.setlanguage"))
            return tab;
        if (args.length == 2 || (args.length == 3) && (s.hasPermission("multilanguageplugin.setlanguage.others") || s.hasPermission("triton.setlanguage.others")))
            for (Language lang : MultiLanguagePlugin.get().getLanguageManager().getAllLanguages())
                if (lang.getName().toLowerCase().startsWith(args[1].toLowerCase()))
                    tab.add(lang.getName());
        return tab;
    }

}
