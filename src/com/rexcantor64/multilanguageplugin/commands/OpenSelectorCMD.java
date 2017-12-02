package com.rexcantor64.multilanguageplugin.commands;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.guiapi.Gui;
import com.rexcantor64.multilanguageplugin.guiapi.GuiButton;
import com.rexcantor64.multilanguageplugin.guiapi.ScrollableGui;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.language.LanguageManager;
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

        if (!p.hasPermission("multilanguageplugin.openselector")) {
            p.sendMessage(SpigotMLP.get().getMessage("error.no-permission"));
            return true;
        }

        openLanguagesSelectionGUI(p);
        return true;
    }

    private void openLanguagesSelectionGUI(Player p) {
        LanguageManager language = SpigotMLP.get().getLanguageManager();
        Language pLang = SpigotMLP.get().getPlayerManager().get(p).getLang();
        Gui gui = new ScrollableGui(SpigotMLP.get().getMessage("other.selector-gui-name"));
        for (Language lang : language.getAllLanguages())
            gui.addButton(new GuiButton(lang.getStack(pLang.equals(lang))).setListener(event -> {
                SpigotMLP.get().getPlayerManager().get(p).setLang(lang);
                p.closeInventory();
                p.sendMessage(SpigotMLP.get().getMessage("success.selector", lang.getDisplayName()));
            }));
        gui.open(p);
    }

}
