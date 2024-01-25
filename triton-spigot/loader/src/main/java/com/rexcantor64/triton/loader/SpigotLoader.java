package com.rexcantor64.triton.loader;

import com.rexcantor64.triton.loader.utils.JarInJarClassLoader;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import me.lucko.jarrelocator.Relocation;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class SpigotLoader extends JavaPlugin {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";
    private static final String SPIGOT_JAR_NAME = "triton-spigot.jarinjar";
    private static final String BOOTSTRAP_CLASS = "com.rexcantor64.triton.spigot.plugin.SpigotPlugin";

    private final LoaderBootstrap plugin;

    public SpigotLoader() {
        boolean relocateAdventure = shouldRelocateAdventure();
        List<Relocation> relocations = new ArrayList<>();
        if (relocateAdventure) {
            getLogger().log(Level.INFO, "Adventure not found or is outdated: relocating it inside Triton");
            relocations.add(new Relocation("net/kyori/adventure", "com/rexcantor64/triton/lib/adventure"));
        } else {
            getLogger().log(Level.INFO, "Found up-to-date version of Adventure! Using server's Adventure library");
        }
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), relocations, CORE_JAR_NAME, SPIGOT_JAR_NAME);
        this.plugin = loader.instantiatePlugin(BOOTSTRAP_CLASS, JavaPlugin.class, this);
    }

    private boolean shouldRelocateAdventure() {
        // TODO manual override

        try {
            // Class only available on adventure 4.15.0+
            Class.forName("net/kyori/adventure/resource/ResourcePackCallback");

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
