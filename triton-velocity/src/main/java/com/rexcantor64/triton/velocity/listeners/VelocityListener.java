package com.rexcantor64.triton.velocity.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import lombok.val;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VelocityListener {

    private MessageParser parser() {
        return Triton.get().getMessageParser();
    }

    private boolean shouldNotTranslateMotd() {
        return !Triton.get().getConfig().isMotd();
    }

    private FeatureSyntax getMotdSyntax() {
        return Triton.get().getConfig().getMotdSyntax();
    }

    @Subscribe
    public void onServerConnect(ServerConnectedEvent e) {
        val lp = VelocityTriton.asVelocity().getPlayerManager().get(e.getPlayer().getUniqueId());

        VelocityTriton.asVelocity().getBridgeManager().sendPlayerLanguage(lp, e.getServer());

        if (Triton.get().getConfig().isRunLanguageCommandsOnLogin())
            lp.executeCommands(e.getServer());
    }

    @Subscribe
    public void afterServerConnect(ServerPostConnectEvent e) {
        if (e.getPlayer().getCurrentServer().isPresent())
            VelocityTriton.asVelocity().getBridgeManager().executeQueue(e.getPlayer().getCurrentServer().get().getServer());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent e) {
        val player = e.getPlayer();
        val lp = new VelocityLanguagePlayer(player);
        VelocityTriton.asVelocity().getPlayerManager().registerPlayer(lp);
        lp.injectNettyPipeline();
    }

    @Subscribe
    public void onPlayerSettingsUpdate(PlayerSettingsChangedEvent event) {
        val lp = Triton.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        if (lp.isWaitingForClientLocale()) {
            lp.setLang(Triton.get().getLanguageManager().getLanguageByLocaleOrDefault(event.getPlayerSettings().getLocale().toString()));
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyPing(ProxyPingEvent event) {
        if (shouldNotTranslateMotd()) {
            return;
        }

        val serverPing = event.getPing().asBuilder();
        val ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        val language = Triton.get().getStorage().getLanguageFromIp(ip);

        if (serverPing.getDescriptionComponent().isPresent()) {
            parser().translateComponent(serverPing.getDescriptionComponent().get(), language, getMotdSyntax())
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(serverPing::description);
        }

        val newSamplePlayers = serverPing.getSamplePlayers().stream()
                .flatMap(player ->
                        parser()
                                .translateString(player.getName(), language, getMotdSyntax())
                                .mapToObj(
                                        translatedName -> {
                                            val translatedNameSplit = translatedName.split("\n", -1);
                                            if (translatedNameSplit.length > 1) {
                                                return Arrays.stream(translatedNameSplit)
                                                        .map(name -> new ServerPing.SamplePlayer(name, UUID.randomUUID()));
                                            } else {
                                                return Stream.of(new ServerPing.SamplePlayer(translatedName, player.getId()));
                                            }
                                        },
                                        () -> Stream.of(player),
                                        Stream::empty
                                )
                )
                .collect(Collectors.toList());
        serverPing.clearSamplePlayers().getSamplePlayers().addAll(newSamplePlayers);

        val version = serverPing.getVersion();
        parser()
                .translateString(
                        version.getName(),
                        language,
                        getMotdSyntax()
                )
                .ifChanged(result -> serverPing.version(new ServerPing.Version(version.getProtocol(), result)));

        event.setPing(serverPing.build());
    }

}
