package com.rexcantor64.multilanguageplugin.listeners;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.SpigotMLP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        MultiLanguagePlugin.get().getPlayerManager().get(e.getPlayer()).setInterceptor(MultiLanguagePlugin.get().getProtocolLibListener());
    }

}
