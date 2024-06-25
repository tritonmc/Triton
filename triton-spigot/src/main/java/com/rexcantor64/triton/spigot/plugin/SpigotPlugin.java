package com.rexcantor64.triton.spigot.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.dependencies.DependencyManager;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.JavaLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.terminal.Log4jInjector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.byteflux.libby.BukkitLibraryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.util.Set;

@RequiredArgsConstructor
public class SpigotPlugin implements PluginLoader, LoaderBootstrap {
    private TritonLogger logger;
    @Getter
    private final JavaPlugin plugin;
    @Getter
    private final Set<LoaderFlag> loaderFlags;
    @Getter
    private DependencyManager dependencyManager;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.plugin.getLogger());
        this.dependencyManager = new DependencyManager(new BukkitLibraryManager(this.getPlugin()), loaderFlags);

        this.dependencyManager.init();
        this.dependencyManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD);

        new SpigotTriton(this).onEnable();
    }

    @Override
    public void onDisable() {
        if (Triton.get().getConfig().isTerminal())
            Log4jInjector.uninjectAppender();
    }

    @Override
    public Platform getPlatform() {
        return Platform.SPIGOT;
    }

    @Override
    public InputStream getResourceAsStream(String fileName) {
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }

    @Override
    public TritonLogger getTritonLogger() {
        return this.logger;
    }
}
