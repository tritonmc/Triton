package com.rexcantor64.multilanguageplugin.listeners;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        SpigotMLP.get().getPlayerManager().get(e.getPlayer()).setInterceptor(SpigotMLP.get().getProtocolLibListener());
    }

}
