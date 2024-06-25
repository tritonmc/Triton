package com.rexcantor64.triton.velocity.plugin;

import com.rexcantor64.triton.dependencies.DependencyManager;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.SLF4JLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.velocity.VelocityTriton;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import net.byteflux.libby.VelocityLibraryManager;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Set;

@Getter
public class VelocityPlugin implements PluginLoader, LoaderBootstrap {
    @Getter
    private final Object plugin;
    private final ProxyServer server;
    private final TritonLogger tritonLogger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    @Getter
    private final Set<LoaderFlag> loaderFlags;
    @Getter
    private final DependencyManager dependencyManager;

    public VelocityPlugin(Object loader, ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Set<LoaderFlag> loaderFlags) {
        this.plugin = loader;
        this.server = server;
        this.tritonLogger = new SLF4JLogger(logger);
        this.dataDirectory = dataDirectory;
        this.loaderFlags = loaderFlags;
        this.dependencyManager = new DependencyManager(new VelocityLibraryManager<>(logger, dataDirectory, server.getPluginManager(), loader), loaderFlags);

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
        getDependencyManager().init();

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
