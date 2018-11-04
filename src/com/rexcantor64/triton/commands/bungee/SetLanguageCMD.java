package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.Language;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class SetLanguageCMD implements CommandExecutor {

    @Override
    @SuppressWarnings("deprecation")
    public void execute(CommandSender s, String[] args) {
        if (!s.hasPermission("multilanguageplugin.setlanguage") && !s.hasPermission("triton.setlanguage")) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.setlanguage"));
            return;
        }

        if (args.length == 1) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("help.setlanguage", "&cUse /%1 setlanguage [player] <language name>", "triton"));
            return;
        }

        ProxiedPlayer target;
        String langName;

        if (args.length >= 3) {
            langName = args[2];
            if (s.hasPermission("multilanguageplugin.setlanguage.others") || s.hasPermission("triton.setlanguage.others")) {
                target = BungeeCord.getInstance().getPlayer(args[1]);
                if (target == null) {
                    s.sendMessage(MultiLanguagePlugin.get().getMessage("error.player-not-found", "&cPlayer %1 not found!", args[1]));
                    return;
                }
            } else {
                s.sendMessage(MultiLanguagePlugin.get().getMessage("error.no-permission", "&cNo permission. Permission required: &4%1", "triton.setlanguage.others"));
                return;
            }
        } else if (s instanceof ProxiedPlayer) {
            target = (ProxiedPlayer) s;
            langName = args[1];
        } else {
            s.sendMessage("Only Players.");
            return;
        }

        Language lang = MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(langName, false);
        if (lang == null) {
            s.sendMessage(MultiLanguagePlugin.get().getMessage("error.lang-not-found", "&cLanguage %1 not found! Note: It's case sensitive. Use TAB to show all the available languages.", args[1]));
            return;
        }

        MultiLanguagePlugin.get().getPlayerManager().get(target.getUniqueId()).setLang(lang);
        if (target == s)
            s.sendMessage(MultiLanguagePlugin.get().getMessage("success.setlanguage", "&aYour language has been changed to %1", lang.getDisplayName()));
        else
            s.sendMessage(MultiLanguagePlugin.get().getMessage("success.setlanguage-others", "&a%1's language has been changed to %2", target.getName(),
                    lang.getDisplayName()));
    }
}
