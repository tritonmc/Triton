package com.rexcantor64.triton.loader;

import com.google.inject.Inject;
import com.rexcantor64.triton.loader.utils.CommonLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "triton",
        name = "Triton",
        url = "https://triton.rexcantor64.com",
        description = "A plugin that replaces any message on your server, to the receiver's language, in real time!",
        version = "@version@",
        authors = {"Rexcantor64"}
)
public class VelocityLoader {
    private static final String PLATFORM_JAR_NAME = "triton-velocity.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.velocity.plugin.VelocityPlugin";

    private final LoaderBootstrap plugin;

    @Inject
    public VelocityLoader(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.plugin = CommonLoader.builder()
                .jarInJarName(PLATFORM_JAR_NAME)
                .bootstrapClassName(BOOTSTRAP_CLASS)
                .constructorType(Object.class)
                .constructorType(ProxyServer.class)
                .constructorType(Logger.class)
                .constructorType(Path.class)
                .constructorValue(this)
                .constructorValue(server)
                .constructorValue(logger)
                .constructorValue(dataDirectory)
                .build()
                .loadPlugin();
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        this.plugin.onEnable();
    }

}
