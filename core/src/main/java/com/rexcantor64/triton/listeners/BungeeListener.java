package com.rexcantor64.triton.listeners;

import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.SocketUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Arrays;

public class BungeeListener implements Listener {

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        BungeeLanguagePlayer lp = (BungeeLanguagePlayer) Triton.get().getPlayerManager()
                .get(event.getPlayer().getUniqueId());

        Triton.asBungee().getBridgeManager().sendPlayerLanguage(lp, event.getPlayer(), event.getServer());

        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            lp.executeCommands(event.getServer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        Plugin plugin = Triton.get().getLoader().asBungee();
        event.registerIntent(plugin);
        Triton.asBungee().getBungeeCord().getScheduler().runAsync(plugin, () -> {
            BungeeLanguagePlayer lp = Triton.get().getPlayerManager()
                    .registerBungee(event.getConnection().getUniqueId(), new BungeeLanguagePlayer(event.getConnection()
                            .getUniqueId(), event.getConnection()));
            BungeeMLP.asBungee().injectPipeline(lp, event.getConnection());
            event.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        Triton.get().getPlayerManager().unregisterPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMotd(ProxyPingEvent event) {
        Plugin plugin = Triton.get().getLoader().asBungee();
        event.registerIntent(plugin);

        if (!Triton.get().getConf().isMotd()) {
            event.completeIntent(plugin);
            return;
        }

        Triton.asBungee().getBungeeCord().getScheduler().runAsync(plugin, () -> {
            event.getResponse().setDescriptionComponent(componentArrayToSingle(Triton.get().getLanguageParser()
                    .parseComponent(Triton.get().getStorage()
                            .getLanguageFromIp(SocketUtils.getIpAddress(event.getConnection().getSocketAddress()))
                            .getName(), Triton.get().getConf().getMotdSyntax(), event.getResponse()
                            .getDescriptionComponent())));
            event.completeIntent(plugin);
        });
    }

    private BaseComponent componentArrayToSingle(BaseComponent... c) {
        if (c.length == 1) return c[0];
        BaseComponent result = new TextComponent("");
        result.setExtra(Arrays.asList(c));
        return result;
    }

}
