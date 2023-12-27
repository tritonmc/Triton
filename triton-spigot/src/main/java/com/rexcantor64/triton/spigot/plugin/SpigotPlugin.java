package com.rexcantor64.triton.spigot.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
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

@RequiredArgsConstructor
public class SpigotPlugin implements PluginLoader, LoaderBootstrap {
    private TritonLogger logger;
    @Getter
    private final JavaPlugin plugin;

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.plugin.getLogger());
        BukkitLibraryManager libraryManager = new BukkitLibraryManager(this.plugin);
        libraryManager.addRepository("https://repo.diogotc.com/mirror/");
        libraryManager.loadLibrary(Dependency.ADVENTURE.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_GSON.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_LEGACY.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_PLAIN.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_KEY.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_JSON.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_MINI_MESSAGE.getLibrary());
        libraryManager.loadLibrary(Dependency.KYORI_EXAMINATION.getLibrary());
        libraryManager.loadLibrary(Dependency.KYORI_OPTION.getLibrary());

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
