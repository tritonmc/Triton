package com.rexcantor64.triton.velocity.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.MessageParser;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
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

    private boolean shouldNotTranslateKick() {
        return !Triton.get().getConfig().isKick();
    }

    private FeatureSyntax getKickSyntax() {
        return Triton.get().getConfig().getKickSyntax();
    }

    @Subscribe
    public void afterServerConnect(ServerPostConnectEvent e) {
        e.getPlayer().getCurrentServer().ifPresent(serverConnection -> {
            VelocityTriton.asVelocity().getBridgeManager().executeQueue(serverConnection.getServer());

            val lp = (VelocityLanguagePlayer) Triton.get().getPlayerManager().get(e.getPlayer().getUniqueId());
            VelocityTriton.asVelocity().getBridgeManager().sendPlayerLanguage(lp);

            if (lp.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_3) > 0) {
                // On 1.20.6 and above, the notchian client crashes if a bossbar that does not exist client-side is updated
                lp.clearCachedBossbars();
            }

            if (Triton.get().getConf().isRunLanguageCommandsOnLogin()) {
                lp.executeCommands(serverConnection.getServer());
            }
        });
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
        val lp = VelocityTriton.asVelocity().getPlayerManager().get(event.getPlayer().getUniqueId());
        lp.setClientLocale(event.getPlayerSettings().getLocale().toString());
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

    @Subscribe(order = PostOrder.LAST)
    public void onPreLogin(PreLoginEvent event) {
        if (shouldNotTranslateKick()) {
            return;
        }

        val ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        val language = Triton.get().getStorage().getLanguageFromIp(ip);

        if (event.getResult().getReasonComponent().isPresent()) {
            parser().translateComponent(event.getResult().getReasonComponent().get(), language, getKickSyntax())
                    .getResultOrToRemove(Component::empty)
                    .ifPresent(result -> event.setResult(PreLoginEvent.PreLoginComponentResult.denied(result)));
        }

    }

}
