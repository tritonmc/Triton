package com.rexcantor64.triton.spigot.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.dependencies.Repository;
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
import net.byteflux.libby.LibraryManager;
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
    private LibraryManager libraryManager;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.plugin.getLogger());

        this.libraryManager = new BukkitLibraryManager(this.plugin);
        libraryManager.addRepository(Repository.DIOGOTC_MIRROR);

        if (hasLoaderFlag(LoaderFlag.RELOCATE_ADVENTURE)) {
            loadDependency(Dependency.ADVENTURE);
            loadDependency(Dependency.ADVENTURE_KEY);
            loadDependency(Dependency.KYORI_EXAMINATION);
        }
        loadDependency(Dependency.KYORI_OPTION);
        loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_GSON);
        loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_LEGACY);
        loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_PLAIN);
        loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD);
        loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_JSON);
        loadDependency(Dependency.ADVENTURE_MINI_MESSAGE);

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
