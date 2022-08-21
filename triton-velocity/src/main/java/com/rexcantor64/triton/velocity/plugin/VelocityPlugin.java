package com.rexcantor64.triton.velocity.plugin;

import com.google.inject.Inject;
import com.rexcantor64.triton.logger.SLF4JLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.file.Path;

@Plugin(id = "triton", name = "Triton", url = "https://triton.rexcantor64.com", description =
        "A plugin that replaces any message on your server, to the receiver's language, in real time!",
        version = "@version@",
        authors = {"Rexcantor64"})

@Getter
public class VelocityPlugin implements PluginLoader {
    private final ProxyServer server;
    private final TritonLogger tritonLogger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.tritonLogger = new SLF4JLogger(logger);
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        new VelocityTriton(this).onEnable();
    }

    @Override
    public Platform getPlatform() {
        return Platform.VELOCITY;
    }

    @Override
    public InputStream getResourceAsStream(String fileName) {
        return VelocityPlugin.class.getResourceAsStream("/" + fileName);
    }
}
