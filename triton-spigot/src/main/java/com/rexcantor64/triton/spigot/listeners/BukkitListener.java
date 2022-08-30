package com.rexcantor64.triton.spigot.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class BukkitListener implements Listener {

    private AdventureParser parser() {
        return Triton.get().getMessageParser();
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Triton.get().getPlayerManager().unregisterPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(AsyncPlayerPreLoginEvent loginEvent) {
        val languagePlayer = SpigotTriton.asSpigot().getPlayerManager().get(loginEvent.getUniqueId());
        if (loginEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            parser()
                    .translateString(loginEvent.getKickMessage(), languagePlayer, Triton.get().getConfig().getKickSyntax())
                    .ifChanged(loginEvent::setKickMessage)
                    .ifUnchanged(() -> loginEvent.setKickMessage(""));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginSync(PlayerLoginEvent loginEvent) {
        val languagePlayer = Triton.get().getPlayerManager().get(loginEvent.getPlayer().getUniqueId());
        if (loginEvent.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            parser()
                    .translateString(loginEvent.getKickMessage(), languagePlayer, Triton.get().getConfig().getKickSyntax())
                    .ifChanged(loginEvent::setKickMessage)
                    .ifUnchanged(() -> loginEvent.setKickMessage(""));
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!Triton.get().getConfig().isPreventPlaceholdersInChat()) {
            return;
        }

        String msg = e.getMessage();
        val indexes = parser().getPatternIndexArray(msg, Triton.get().getConfig().getChatSyntax().getLang());
        for (int i = 0; i < indexes.size(); ++i) {
            val index = indexes.get(i);
            // add a zero width space to prevent the parser from finding this placeholder
            // https://en.wikipedia.org/wiki/Zero-width_space
            msg = msg.substring(0, index[0] + 1 + i) + '\u200b' + msg.substring(index[0] + 1 + i);
        }
        e.setMessage(msg);
    }

}
