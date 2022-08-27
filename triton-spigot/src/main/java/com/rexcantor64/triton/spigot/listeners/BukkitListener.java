package com.rexcantor64.triton.spigot.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.LanguageParser;
import com.rexcantor64.triton.spigot.player.SpigotLanguagePlayer;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitListener implements Listener {

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Triton.get().getPlayerManager().unregisterPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent e) {
        val lp = new SpigotLanguagePlayer(e.getUniqueId());
        Triton.asSpigot().getPlayerManager().registerPlayer(lp);
        if (e.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
            e.setKickMessage(Triton.get().getLanguageParser()
                    .replaceLanguages(e.getKickMessage(), lp, Triton.get().getConfig().getKickSyntax()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginSync(PlayerLoginEvent e) {
        val lp = Triton.get().getPlayerManager().get(e.getPlayer().getUniqueId());
        if (e.getResult() != PlayerLoginEvent.Result.ALLOWED)
            e.setKickMessage(Triton.get().getLanguageParser()
                    .replaceLanguages(e.getKickMessage(), lp, Triton.get().getConfig().getKickSyntax()));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!Triton.get().getConfig().isPreventPlaceholdersInChat()) return;

        String msg = e.getMessage();
        val indexes = LanguageParser.getPatternIndexArray(msg, Triton.get().getConfig().getChatSyntax().getLang());
        for (int i = 0; i < indexes.size(); ++i) {
            val index = indexes.get(i);
            msg = msg.substring(0, index[0] + 1 + i) + ChatColor.RESET + msg.substring(index[0] + 1 + i);
        }
        e.setMessage(msg);
    }

}
