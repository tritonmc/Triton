package com.rexcantor64.triton.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.player.SpigotLanguagePlayer;
import lombok.val;
import lombok.var;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
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
            e.setMotd(Triton.get().getLanguageParser().replaceLanguages(e.getMotd(), Triton.get().getStorage()
                    .getLanguageFromIp(e.getAddress().getHostAddress()).getName(), Triton.get().getConf()
                    .getMotdSyntax()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent e) {
        SpigotLanguagePlayer lp = Triton.get().getPlayerManager()
                .registerSpigot(e.getUniqueId(), new SpigotLanguagePlayer(e.getUniqueId()));
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            e.setKickMessage(Triton.get().getLanguageParser()
                    .replaceLanguages(e.getKickMessage(), lp, Triton.get().getConf().getKickSyntax()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginSync(PlayerLoginEvent e) {
        SpigotLanguagePlayer lp = Triton.get().getPlayerManager()
                .registerSpigot(e.getPlayer().getUniqueId(), new SpigotLanguagePlayer(e.getPlayer().getUniqueId()));
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED)
            e.setKickMessage(Triton.get().getLanguageParser()
                    .replaceLanguages(e.getKickMessage(), lp, Triton.get().getConf().getKickSyntax()));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!Triton.get().getConfig().isPreventPlaceholdersInChat()) return;

        var msg = e.getMessage();
        val indexes = LanguageParser.getPatternIndexArray(msg, Triton.get().getConfig().getChatSyntax().getLang());
        for (var i = 0; i < indexes.size(); ++i) {
            val index = indexes.get(i);
            msg = msg.substring(0, index[0] + 1 + i) + ChatColor.RESET + msg.substring(index[0] + 1 + i);
        }
        e.setMessage(msg);
    }

}
