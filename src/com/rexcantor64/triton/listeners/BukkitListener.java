package com.rexcantor64.triton.listeners;

import com.rexcantor64.triton.Triton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Triton.get().getPlayerManager().unregisterPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMotd(ServerListPingEvent e) {
        if (Triton.get().getConf().isMotd())
            e.setMotd(Triton.get().getLanguageParser().replaceLanguages(e.getMotd(), Triton.get().getPlayerStorage()
                    .getLanguageFromIp(e.getAddress().getHostAddress()).getName(), Triton.get().getConf()
                    .getMotdSyntax()));
    }

}
