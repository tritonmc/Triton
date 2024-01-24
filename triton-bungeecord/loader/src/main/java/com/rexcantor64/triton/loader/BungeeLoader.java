package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.JarInJarClassLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import me.lucko.jarrelocator.Relocation;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BungeeLoader extends Plugin {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";
    private static final String SPIGOT_JAR_NAME = "triton-bungeecord.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.bungeecord.plugin.BungeePlugin";

    private final LoaderBootstrap plugin;

    public BungeeLoader() {
        List<Relocation> relocations = new ArrayList<>();
            relocations.add(new Relocation("net/kyori/adventure", "com/rexcantor64/triton/lib/adventure"));
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), relocations, CORE_JAR_NAME, SPIGOT_JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, Plugin.class, this);
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