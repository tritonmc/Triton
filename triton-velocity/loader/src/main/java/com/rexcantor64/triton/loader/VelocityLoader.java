package com.rexcantor64.triton.loader;

import com.google.inject.Inject;
import com.rexcantor64.triton.loader.utils.JarInJarClassLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collections;

@Plugin(id = "triton", name = "Triton", url = "https://triton.rexcantor64.com", description =
        "A plugin that replaces any message on your server, to the receiver's language, in real time!",
        version = "@version@",
        authors = {"Rexcantor64"})
public class VelocityLoader {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";
    private static final String VELOCITY_JAR_NAME = "triton-velocity.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    @Inject
    public VelocityLoader(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), Collections.emptyList(), CORE_JAR_NAME, VELOCITY_JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS,
                new Class[]{ProxyServer.class, Logger.class, Path.class},
                new Object[]{server, logger, dataDirectory});
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        this.plugin.onEnable();
    }

}
