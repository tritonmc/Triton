package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.CommonLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;

public class SpigotLoader extends JavaPlugin {
    private static final String PLATFORM_JAR_NAME = "triton-spigot.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    public SpigotLoader() {
        val builder = CommonLoader.builder()
                .jarInJarName(PLATFORM_JAR_NAME)
                .bootstrapClassName(BOOTSTRAP_CLASS)
                .constructorType(JavaPlugin.class)
                .constructorValue(this)
                .flag(LoaderFlag.SHADE_ADVENTURE);

        if (shouldRelocateAdventure()) {
            builder.flag(LoaderFlag.RELOCATE_ADVENTURE);
        }

        this.plugin = builder.build().loadPlugin();
    }

    private boolean shouldRelocateAdventure() {
        // TODO manual override

        try {
            // Class only available on adventure 4.15.0+
            Class.forName("net.kyori.adventure.resource.ResourcePackCallback");

            // A modern version of adventure is already present
            return false;
        } catch (ClassNotFoundException ignore) {
            // Adventure is not present or an outdated version is present
            return true;
        }
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
