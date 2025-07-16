package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.CommonLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.InvocationTargetException;

public class SpigotLoader extends JavaPlugin {
    private static final String PLATFORM_JAR_NAME = "triton-spigot.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    public SpigotLoader() {
        val builder = CommonLoader.builder()
                .jarInJarName(PLATFORM_JAR_NAME)
                .bootstrapClassName(BOOTSTRAP_CLASS)
                .constructorType(JavaPlugin.class)
                .constructorValue(this);

        if (!isModernPaper()) {
            builder.flag(LoaderFlag.SHADE_ADVENTURE);
            if (shouldRelocateAdventure()) {
                builder.flag(LoaderFlag.RELOCATE_ADVENTURE);
            }
        }

        this.plugin = builder
                .build()
                .loadUserLoaderFlags(this.getDataFolder().toPath())
                .loadPlugin();
    }

    private boolean isModernPaper() {
        try {
            val server = Bukkit.getServer();
            // this method is only available on Paper (and forks)
            val method = server.getClass().getMethod("getMinecraftVersion");
            String version = method.invoke(server).toString();

            val parts = version.split("\\.");
            val major = Integer.parseInt(parts[0]);
            val minor = Integer.parseInt(parts[1]);
            val patch = Integer.parseInt(parts[2]);

            // Ensure at least Paper 1.21.4 (adventure 4.18, shadow color introduced)
            val wantedMajor = 1;
            val wantedMinor = 21;
            val wantedPatch = 4;
            return major > wantedMajor
                    || (major == wantedMajor && minor > wantedMinor)
                    || (major == wantedMajor && minor == wantedMinor && patch >= wantedPatch);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 IndexOutOfBoundsException | NumberFormatException ignore) {
            // Paper is not present or an outdated version is present
            return false;
        }
    }

    private boolean shouldRelocateAdventure() {
        try {
            // Method only available on adventure 4.22.0+
            Class<?> clickEventClass = Class.forName("net.kyori.adventure.text.event.ClickEvent");
            clickEventClass.getMethod("payload");

            // A modern version of adventure is already present
            return false;
        } catch (ClassNotFoundException | NoSuchMethodException ignore) {
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
