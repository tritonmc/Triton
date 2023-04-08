package com.rexcantor64.triton.spigot.commands;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;
import org.bukkit.Bukkit;

import java.util.Objects;

public class GetFlagCommand extends com.rexcantor64.triton.commands.GetFlagCommand {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, PlayerOnlyCommandException, UnsupportedPlatformException {
        super.handleCommand(event);
        val sender = event.getSender();
        val uuid = sender.getUUID();

        sender.assertPermission("triton.getflag", "multilanguageplugin.getflag");

        if (event.getArgs().length == 0) {
            sender.sendMessageFormatted("help.getflag", event.getLabel());
            return;
        }

        val lang = Triton.get().getLanguageManager().getLanguageByName(event.getArgs()[0], false);
        if (lang == null) {
            sender.sendMessageFormatted("error.lang-not-found", event.getArgs()[0]);
            return;
        }

        Objects.requireNonNull(Bukkit.getPlayer(uuid))
                .getInventory()
                .addItem(SpigotTriton.asSpigot().getBannerBuilder().fromLanguage(lang, false));
        sender.sendMessageFormatted("success.getflag", lang.getDisplayName());
    }

}
