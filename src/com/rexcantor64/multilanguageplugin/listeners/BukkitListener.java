package com.rexcantor64.multilanguageplugin.listeners;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.player.SpigotLanguagePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        ((SpigotLanguagePlayer) MultiLanguagePlugin.asSpigot().getPlayerManager().get(e.getPlayer().getUniqueId())).setInterceptor(MultiLanguagePlugin.get().getProtocolLibListener());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        MultiLanguagePlugin.get().getPlayerManager().unregisterPlayer(e.getPlayer().getUniqueId());
    }

}
