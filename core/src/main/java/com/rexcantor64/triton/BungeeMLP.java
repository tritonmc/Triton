package com.rexcantor64.triton;

import com.rexcantor64.triton.bridge.BungeeBridgeManager;
import com.rexcantor64.triton.commands.handler.BungeeCommand;
import com.rexcantor64.triton.commands.handler.BungeeCommandHandler;
import com.rexcantor64.triton.packetinterceptor.BungeeDecoder;
import com.rexcantor64.triton.packetinterceptor.BungeeListener;
import com.rexcantor64.triton.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.plugin.BungeePlugin;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.terminal.Log4jInjector;
import com.rexcantor64.triton.utils.NMSUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.netty.PipelineUtils;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SingleLineChart;

import java.io.File;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeMLP extends Triton {

    @Getter
    private BungeeBridgeManager bridgeManager;
    private ScheduledTask configRefreshTask;

    public BungeeMLP(PluginLoader loader) {
        super.loader = loader;
    }

    public BungeePlugin getLoader() {
        return (BungeePlugin) this.loader;
    }

    @Override
    public void onEnable() {
        instance = this;
        super.onEnable();

        Metrics metrics = new Metrics(getLoader(), 5607);
        metrics.addCustomChart(new SingleLineChart("active_placeholders",
                () -> Triton.get().getLanguageManager().getItemCount()));

        bridgeManager = new BungeeBridgeManager();
        getBungeeCord().getPluginManager().registerListener(getLoader(), bridgeManager);
        getBungeeCord().getPluginManager()
                .registerListener(getLoader(), new com.rexcantor64.triton.listeners.BungeeListener());
        getBungeeCord().registerChannel("triton:main");

        for (ProxiedPlayer p : getBungeeCord().getPlayers()) {
            BungeeLanguagePlayer lp = (BungeeLanguagePlayer) getPlayerManager().get(p.getUniqueId());
            injectPipeline(lp, p);
        }

        val commandHandler = new BungeeCommandHandler();
        getBungeeCord().getPluginManager()
                .registerCommand(getLoader(), new BungeeCommand(commandHandler, "triton", getConfig()
                        .getCommandAliases()
                        .toArray(new String[0])));
        getBungeeCord().getPluginManager()
                .registerCommand(getLoader(), new BungeeCommand(commandHandler, "twin"));

        if (getStorage() instanceof LocalStorage)
            bridgeManager.sendConfigToEveryone();

        try {
            if (getConf().isTerminal())
                BungeeTerminalManager.injectTerminalFormatter();
        } catch (Error | Exception e) {
            try {
                if (getConf().isTerminal())
                    Log4jInjector.injectAppender();
            } catch (Error | Exception e1) {
                getLogger()
                        .logError(e, "Failed to inject terminal translations. Some forked BungeeCord servers might not " +
                                "work correctly. To hide this message, disable terminal translation on config.");
                getLogger().logError(e1, "");
            }
        }
    }

    @Override
    public void reload() {
        super.reload();
        if (bridgeManager != null)
            bridgeManager.sendConfigToEveryone();
    }

    @Override
    protected void startConfigRefreshTask() {
        if (configRefreshTask != null) configRefreshTask.cancel();
        if (getConf().getConfigAutoRefresh() <= 0) return;
        configRefreshTask = getBungeeCord().getScheduler()
                .schedule(getLoader(), this::reload, getConf().getConfigAutoRefresh(), TimeUnit.SECONDS);
    }


    public File getDataFolder() {
        return getLoader().getDataFolder();
    }

    @Override
    public String getVersion() {
        return getLoader().getDescription().getVersion();
    }

    public void injectPipeline(BungeeLanguagePlayer lp, Connection p) {
        try {
            Object ch = NMSUtils.getDeclaredField(p, "ch");
            Method method = ch.getClass().getDeclaredMethod("getHandle");
            Channel channel = (Channel) method.invoke(ch, new Object[0]);
            channel.pipeline().addAfter(PipelineUtils.PACKET_DECODER, "triton-custom-decoder", new BungeeDecoder(lp));
            channel.pipeline()
                    .addAfter(PipelineUtils.PACKET_ENCODER, "triton-custom-encoder", new BungeeListener(lp));
            channel.pipeline().remove("triton-pre-login-encoder");
        } catch (Exception e) {
            getLogger().logError("[PacketInjector] Failed to inject client connection for %1", lp.getUUID());
            e.printStackTrace();
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        getBungeeCord().getScheduler().runAsync(getLoader(), runnable);
    }

    public ProxyServer getBungeeCord() {
        return getLoader().getProxy();
    }

    @Override
    public UUID getPlayerUUIDFromString(String input) {
        val player = getBungeeCord().getPlayer(input);
        if (player != null) return player.getUniqueId();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public short getMcVersion() {
        return 0;
    }

    @Override
    public short getMinorMcVersion() {
        return 0;
    }
}
