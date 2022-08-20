package com.rexcantor64.triton.velocity.listeners;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.VelocityLanguagePlayer;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import lombok.val;

public class VelocityListener {

    @Subscribe
    public void onServerConnect(ServerConnectedEvent e) {
        val lp = (VelocityLanguagePlayer) Triton.get().getPlayerManager().get(e.getPlayer().getUniqueId());

        Triton.asVelocity().getBridgeManager().sendPlayerLanguage(lp, e.getServer());

        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            lp.executeCommands(e.getServer());
    }

    @Subscribe
    public void afterServerConnect(ServerPostConnectEvent e) {
        if (e.getPlayer().getCurrentServer().isPresent())
            Triton.asVelocity().getBridgeManager().executeQueue(e.getPlayer().getCurrentServer().get().getServer());
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLogin(LoginEvent e) {
        val player = e.getPlayer();
        val lp = new VelocityLanguagePlayer(player);
        Triton.get().getPlayerManager().registerPlayer(lp);
    }

    @Subscribe
    public void onPlayerSettingsUpdate(PlayerSettingsChangedEvent event) {
        val lp = Triton.get().getPlayerManager().get(event.getPlayer().getUniqueId());
        if (lp.isWaitingForClientLocale()) {
            lp.setLang(Triton.get().getLanguageManager().getLanguageByLocale(event.getPlayerSettings().getLocale().toString(), true));
        }
    }

}
