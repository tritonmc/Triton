package com.rexcantor64.triton.velocity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.velocity.bridge.VelocityBridgeManager;
import com.rexcantor64.triton.velocity.commands.handler.VelocityCommandHandler;
import com.rexcantor64.triton.velocity.listeners.VelocityListener;
import com.rexcantor64.triton.velocity.packetinterceptor.VelocityPacketEventsManager;
import com.rexcantor64.triton.velocity.player.VelocityLanguagePlayer;
import com.rexcantor64.triton.velocity.plugin.VelocityPlugin;
import com.rexcantor64.triton.velocity.scheduler.VelocityScheduler;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class VelocityTriton extends Triton<VelocityLanguagePlayer, VelocityBridgeManager> {

    @Getter
    private ChannelIdentifier bridgeChannelIdentifier;

    public VelocityTriton(PluginLoader loader) {
        super(
                new PlayerManager<>(VelocityLanguagePlayer::new),
                new VelocityBridgeManager(),
                new VelocityScheduler(((VelocityPlugin) loader).getServer(), ((VelocityPlugin) loader).getPlugin())
        );
        super.loader = loader;
    }

    public static VelocityTriton asVelocity() {
        return (VelocityTriton) instance;
    }

    public VelocityPlugin getLoader() {
        return (VelocityPlugin) this.loader;
    }

    public Object getPlugin() {
        return getLoader().getPlugin();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        // bStats
        Metrics metrics = getLoader().getMetricsFactory().make(getPlugin(), 16222);
        getBStatsCustomCharts().forEach(metrics::addCustomChart);

        val eventManager = getVelocity().getEventManager();
        eventManager.register(getPlugin(), new VelocityListener());
        eventManager.register(getPlugin(), bridgeManager);

        this.bridgeChannelIdentifier = MinecraftChannelIdentifier.create("triton", "main");
        getVelocity().getChannelRegistrar().register(this.bridgeChannelIdentifier);

        if (getStorage() instanceof LocalStorage)
            bridgeManager.sendConfigToEveryone();

        val commandHandler = new VelocityCommandHandler();
        val commandManager = getLoader().getServer().getCommandManager();
        commandManager.register(commandManager.metaBuilder("triton")
                .aliases(getConfig().getCommandAliases().toArray(new String[0])).build(), commandHandler);
        commandManager.register(commandManager.metaBuilder("twin").build(), commandHandler);
    }

    @Override
    public void reload() {
        super.reload();
        if (bridgeManager != null && bridgeChannelIdentifier != null) {
            bridgeManager.sendConfigToEveryone();
        }
    }

    @Override
    protected void initPacketEventsManager() {
        this.packetEventsManager = new VelocityPacketEventsManager();
    }

    public File getDataFolder() {
        return getLoader().getDataDirectory().toFile();
    }

    @Override
    public @NonNull String getVersion() {
        return getLoader().getPluginContainer().getDescription().getVersion().orElse("unknown");
    }

    public ProxyServer getVelocity() {
        return getLoader().getServer();
    }

    @Override
    public UUID getPlayerUUIDFromString(String input) {
        val player = getVelocity().getPlayer(input);
        if (player.isPresent()) return player.get().getUniqueId();

        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected String getConfigFileName() {
        return "config_velocity";
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
        return getLoader().getServer().getPluginManager().getPlugin("packetevents")
                .map(PluginContainer::getDescription)
                .flatMap(PluginDescription::getVersion);
    }

    @Override
    public @NotNull JsonElement getPlatformDebugInfo() {
        val obj = new JsonObject();
        val version = getLoader().getServer().getVersion();
        obj.addProperty("serverName", version.getName());
        obj.addProperty("serverVersion", version.getVersion());
        obj.addProperty("serverVendor", version.getVendor());
        getPacketEventsVersion().ifPresent(v -> obj.addProperty("packetEventsVersion", v));
        return obj;
    }
}
