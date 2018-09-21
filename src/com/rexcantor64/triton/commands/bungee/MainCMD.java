package com.rexcantor64.triton.commands.bungee;

import com.rexcantor64.triton.MultiLanguagePlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

public class MainCMD extends Command {

    public MainCMD() {
        super("bmultilanguageplugin", "triton.reload", "bmlp", "bml", "bmultilanguage", "blanguage", "blang", "btriton");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        MultiLanguagePlugin.get().reload();
        commandSender.sendMessage(new TextComponent(ChatColor.GREEN + "Config reloaded!"));
    }
}
