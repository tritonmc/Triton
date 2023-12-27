package com.rexcantor64.triton.velocity.plugin;

import com.google.inject.Inject;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

@Getter
public class VelocityPlugin implements PluginLoader, LoaderBootstrap {
    private final ProxyServer server;
    private final TritonLogger tritonLogger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.tritonLogger = new SLF4JLogger(logger);
        this.dataDirectory = dataDirectory;

        try {
            // Because the loader module does not depend on bStats, we have to do this instead
            Constructor<?> constructor = Metrics.Factory.class.getDeclaredConstructor(ProxyServer.class, Logger.class, Path.class);
            constructor.setAccessible(true);
            this.metricsFactory = (Metrics.Factory) constructor.newInstance(server, logger, dataDirectory);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new RuntimeException("Failed to initialize Metrics factory", e);
        }
    }

    @Override
    public void onEnable() {
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
