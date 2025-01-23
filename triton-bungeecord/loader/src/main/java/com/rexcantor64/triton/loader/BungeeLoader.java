package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.CommonLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeeLoader extends Plugin {
    private static final String PLATFORM_JAR_NAME = "triton-bungeecord.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.bungeecord.plugin.BungeePlugin";

    private final LoaderBootstrap plugin;

    public BungeeLoader() {
        this.plugin = CommonLoader.builder()
                .jarInJarName(PLATFORM_JAR_NAME)
                .bootstrapClassName(BOOTSTRAP_CLASS)
                .constructorType(Plugin.class)
                .constructorValue(this)
                .flag(LoaderFlag.SHADE_ADVENTURE)
                .flag(LoaderFlag.RELOCATE_ADVENTURE)
                .build()
                .loadPlugin();
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
