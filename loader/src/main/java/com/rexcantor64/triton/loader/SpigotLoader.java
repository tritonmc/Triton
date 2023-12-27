package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.JarInJarClassLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotLoader extends JavaPlugin {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";
    private static final String SPIGOT_JAR_NAME = "triton-spigot.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    public SpigotLoader() {
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), CORE_JAR_NAME, SPIGOT_JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, JavaPlugin.class, this);
    }

    @Override
    public void onLoad() {
        this.plugin.onLoad();
    }

    @Override
    public void onEnable() {
        this.plugin.onEnable();
    }

    @Override
    public void onDisable() {
        this.plugin.onDisable();
    }
}
