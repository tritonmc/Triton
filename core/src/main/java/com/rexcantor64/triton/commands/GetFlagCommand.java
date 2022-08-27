package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.spigot.wrappers.items.ItemStackParser;
import lombok.val;
import org.bukkit.Bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class GetFlagCommand implements Command {

    @Override
    public boolean handleCommand(CommandEvent event) {
        val sender = event.getSender();
        val uuid = sender.getUUID();

        if (uuid == null) {
            sender.sendMessage("Only players");
            return true;
        }

        if (event.getPlatform() != Platform.SPIGOT) {
            return false;
        }

        sender.assertPermission("triton.getflag", "multilanguageplugin.getflag");

        if (event.getArgs().length == 0) {
            sender.sendMessageFormatted("help.getflag", event.getLabel());
            return true;
        }

        val lang = Triton.get().getLanguageManager().getLanguageByName(event.getArgs()[0], false);
        if (lang == null) {
            sender.sendMessageFormatted("error.lang-not-found", event.getArgs()[0]);
            return true;
        }

        Objects.requireNonNull(Bukkit.getPlayer(uuid))
                .getInventory()
                .addItem(ItemStackParser.bannerToItemStack(lang.getBanner(), false));
        sender.sendMessageFormatted("success.getflag", lang.getDisplayName());

        return true;
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        event.getSender().assertPermission("triton.getflag", "multilanguageplugin.getflag");

        if (event.getArgs().length != 1) return Collections.emptyList();

        return Triton.get().getLanguageManager().getAllLanguages().stream().map(Language::getName)
                .filter(name -> name.toLowerCase().startsWith(event.getArgs()[0])).collect(Collectors.toList());
    }
}
