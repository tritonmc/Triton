package com.rexcantor64.triton.bungeecord.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.utils.BaseComponentUtils;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.packetinterceptor.PreLoginBungeeEncoder;
import com.rexcantor64.triton.bungeecord.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.utils.ReflectionUtils;
import com.rexcantor64.triton.utils.SocketUtils;
import io.netty.channel.Channel;
import lombok.val;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.md_5.bungee.netty.PipelineUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

public class BungeeListener implements Listener {

    @EventHandler
    public void onPlayerJoin(ServerConnectedEvent event) {
        BungeeLanguagePlayer lp = BungeeTriton.asBungee().getPlayerManager()
                .get(event.getPlayer().getUniqueId());
        Triton.get().getLogger().logTrace("Player %1 connected to a new server", lp);

        BungeeTriton.asBungee().getBridgeManager().sendPlayerLanguage(lp, event.getServer());

        if (Triton.get().getConfig().isRunLanguageCommandsOnLogin()) {
            lp.executeCommands(event.getServer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(LoginEvent event) {
        if (event.isCancelled()) return;
        Plugin plugin = BungeeTriton.asBungee().getLoader();
        event.registerIntent(plugin);
        BungeeTriton.asBungee().getBungeeCord().getScheduler().runAsync(plugin, () -> {
            val lp = new BungeeLanguagePlayer(event.getConnection().getUniqueId(), event.getConnection());
            BungeeTriton.asBungee().getPlayerManager().registerPlayer(lp);
            BungeeTriton.asBungee().injectPipeline(lp, event.getConnection());
            event.completeIntent(plugin);
        });
    }

    @EventHandler
    public void onLeave(PlayerDisconnectEvent event) {
        Triton.get().getPlayerManager().unregisterPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = -128)
    public void onPreLogin(PlayerHandshakeEvent event) {
        val ip = SocketUtils.getIpAddress(event.getConnection().getSocketAddress());
        try {
            Object ch = ReflectionUtils.getDeclaredField(event.getConnection(), "ch");
            Method method = ch.getClass().getDeclaredMethod("getHandle");
            Channel channel = (Channel) method.invoke(ch, new Object[0]);
            channel.pipeline()
                    .addAfter(PipelineUtils.PACKET_ENCODER, "triton-pre-login-encoder",
                            new PreLoginBungeeEncoder(ip));
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "[PacketInjector] Failed to inject pre-login client connection for %1", ip);
        }
    }

    @EventHandler(priority = 127)
    public void onMotd(ProxyPingEvent event) {
        Plugin plugin = BungeeTriton.asBungee().getLoader();

        if (!Triton.get().getConfig().isMotd())
            return;

        event.registerIntent(plugin);

        BungeeTriton.asBungee().getBungeeCord().getScheduler().runAsync(plugin, () -> {
            val parser = Triton.get().getMessageParser();

            val ipAddress = SocketUtils.getIpAddress(event.getConnection().getSocketAddress());
            val lang = Triton.get().getStorage().getLanguageFromIp(ipAddress);
            Triton.get().getLogger().logTrace("Translating MOTD in language '%1' for IP address '%2'", lang, ipAddress);
            val syntax = Triton.get().getConfig().getMotdSyntax();

            val players = event.getResponse().getPlayers();
            if (players.getSample() != null) {
                val newSample = new ArrayList<ServerPing.PlayerInfo>();
                for (val playerInfo : players.getSample()) {
                    if (playerInfo.getName() == null) {
                        newSample.add(playerInfo);
                        continue;
                    }
                    parser.translateString(playerInfo.getName(), lang, syntax)
                            .ifUnchanged(() -> newSample.add(playerInfo))
                            .ifChanged(translatedName -> {
                                val translatedNameSplit = translatedName.split("\n", -1);
                                if (translatedNameSplit.length > 1) {
                                    for (val split : translatedNameSplit) {
                                        newSample.add(new ServerPing.PlayerInfo(split, UUID.randomUUID()));
                                    }
                                } else {
                                    newSample.add(new ServerPing.PlayerInfo(translatedName, playerInfo.getUniqueId()));
                                }
                            });
                }
                players.setSample(newSample.toArray(new ServerPing.PlayerInfo[0]));
            }

            val response = event.getResponse();
            val version = response.getVersion();
            parser.translateString(version.getName(), lang, syntax)
                    .map(translationVersion -> new ServerPing.Protocol(translationVersion, version.getProtocol()))
                    .ifChanged(response::setVersion);

            parser.translateComponent(
                            BaseComponentUtils.deserialize(event.getResponse().getDescriptionComponent()),
                            lang,
                            syntax
                    )
                    .map(BaseComponentUtils::serialize)
                    .map(BaseComponentUtils::convertArrayToSingle)
                    .ifChanged(response::setDescriptionComponent);

            event.completeIntent(plugin);
        });
    }


}
