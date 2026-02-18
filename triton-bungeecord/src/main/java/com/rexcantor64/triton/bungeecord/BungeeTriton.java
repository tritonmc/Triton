package com.rexcantor64.triton.bungeecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.bridge.BungeeBridgeManager;
import com.rexcantor64.triton.bungeecord.commands.handler.BungeeCommand;
import com.rexcantor64.triton.bungeecord.commands.handler.BungeeCommandHandler;
import com.rexcantor64.triton.bungeecord.packetinterceptor.BungeeDecoder;
import com.rexcantor64.triton.bungeecord.packetinterceptor.BungeeListener;
import com.rexcantor64.triton.bungeecord.packetinterceptor.BungeePacketEventsManager;
import com.rexcantor64.triton.bungeecord.player.BungeeLanguagePlayer;
import com.rexcantor64.triton.bungeecord.plugin.BungeePlugin;
import com.rexcantor64.triton.bungeecord.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.terminal.Log4jInjector;
import com.rexcantor64.triton.utils.ReflectionUtils;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.val;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.netty.PipelineUtils;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BungeeTriton extends Triton<BungeeLanguagePlayer, BungeeBridgeManager> {

    @Getter
    private BungeeBridgeManager bridgeManager;
    private ScheduledTask configRefreshTask;

    public BungeeTriton(PluginLoader loader) {
        super(new PlayerManager<>(BungeeLanguagePlayer::new), new BungeeBridgeManager());
        super.loader = loader;
    }

    public static BungeeTriton asBungee() {
        return (BungeeTriton) instance;
    }

    public Plugin getPlugin() {
        return this.getLoader().getPlugin();
    }

    public BungeePlugin getLoader() {
        return (BungeePlugin) this.loader;
    }

    @Override
    public void onEnable() {
        super.onEnable();

        Metrics metrics = new Metrics(getPlugin(), 5607);
        getBStatsCustomCharts().forEach(metrics::addCustomChart);

        bridgeManager = new BungeeBridgeManager();
        getBungeeCord().getPluginManager().registerListener(getPlugin(), bridgeManager);
        getBungeeCord().getPluginManager()
                .registerListener(getPlugin(), new com.rexcantor64.triton.bungeecord.listeners.BungeeListener());
        getBungeeCord().registerChannel("triton:main");

        if (!this.getConfig().isUsePacketEvents()) {
            for (ProxiedPlayer p : getBungeeCord().getPlayers()) {
                BungeeLanguagePlayer lp = getPlayerManager().get(p.getUniqueId());
                injectPipeline(lp, p, p.getPendingConnection().getVersion());
            }
        }

        val commandHandler = new BungeeCommandHandler();
        getBungeeCord().getPluginManager()
                .registerCommand(getPlugin(), new BungeeCommand(commandHandler, "triton", getConfig()
                        .getCommandAliases()
                        .toArray(new String[0])));
        getBungeeCord().getPluginManager()
                .registerCommand(getPlugin(), new BungeeCommand(commandHandler, "twin"));

        if (getStorage() instanceof LocalStorage)
            bridgeManager.sendConfigToEveryone();

        try {
            if (getConfig().isTerminal())
                BungeeTerminalManager.injectTerminalFormatter();
        } catch (Error | Exception e) {
            try {
                if (getConfig().isTerminal())
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
    protected void initPacketEventsManager() {
        this.packetEventsManager = new BungeePacketEventsManager();
    }

    @Override
    protected void startConfigRefreshTask() {
        if (configRefreshTask != null) configRefreshTask.cancel();
        if (getConfig().getConfigAutoRefresh() <= 0) return;
        configRefreshTask = getBungeeCord().getScheduler()
                .schedule(getPlugin(), this::reload, getConfig().getConfigAutoRefresh(), TimeUnit.SECONDS);
    }


    public File getDataFolder() {
        return getPlugin().getDataFolder();
    }

    @Override
    public String getVersion() {
        return getPlugin().getDescription().getVersion();
    }

    public void injectPipeline(BungeeLanguagePlayer lp, Connection p, int protocolVersion) {
        Triton.get().getLogger().logTrace("Injecting pipeline for player %1", lp);
        try {
            Object ch = ReflectionUtils.getDeclaredField(p, "ch");
            Method method = ch.getClass().getDeclaredMethod("getHandle");
            Channel channel = (Channel) method.invoke(ch, new Object[0]);

            channel.pipeline().addAfter(PipelineUtils.PACKET_DECODER, "triton-custom-decoder", new BungeeDecoder(lp));
            channel.pipeline()
                    .addAfter(PipelineUtils.PACKET_ENCODER, "triton-custom-encoder", new BungeeListener(lp, protocolVersion));
            channel.pipeline().remove("triton-pre-login-encoder");
        } catch (Exception e) {
            getLogger().logError(e, "[PacketInjector] Failed to inject client connection for %1", lp.getUUID());
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        getBungeeCord().getScheduler().runAsync(getPlugin(), runnable);
    }

    public ProxyServer getBungeeCord() {
        return getPlugin().getProxy();
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
    protected String getConfigFileName() {
        return "config_bungeecord";
    }

    @Override
    protected @NotNull List<@NotNull CustomChart> getBStatsCustomCharts() {
        val charts = super.getBStatsCustomCharts();
        charts.add(new SimplePie(
                "packet_interception_backend",
                () -> this.getConfig().isUsePacketEvents() ? "PacketEvents" : "Native"
        ));
        return charts;
    }

    private Optional<String> getPacketEventsVersion() {
        return Optional.ofNullable(getLoader().getPlugin().getProxy().getPluginManager().getPlugin("packetevents"))
                .map(Plugin::getDescription)
                .map(PluginDescription::getVersion);
    }

    @Override
    public @NotNull JsonElement getPlatformDebugInfo() {
        val obj = new JsonObject();
        val proxy = getLoader().getPlugin().getProxy();
        obj.addProperty("serverName", proxy.getName());
        obj.addProperty("serverVersion", proxy.getVersion());
        getPacketEventsVersion().ifPresent(version -> obj.addProperty("packetEventsVersion", version));
        return obj;
    }
}
