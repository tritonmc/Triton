package com.rexcantor64.triton.listeners;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
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
