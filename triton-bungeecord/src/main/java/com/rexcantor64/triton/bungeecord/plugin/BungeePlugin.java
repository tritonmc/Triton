package com.rexcantor64.triton.bungeecord.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.logger.JavaLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.terminal.Log4jInjector;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.byteflux.libby.BungeeLibraryManager;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.InputStream;
import java.util.logging.Level;

@RequiredArgsConstructor
public class BungeePlugin implements PluginLoader, LoaderBootstrap {
    private TritonLogger logger;
    @Getter
    private final Plugin plugin;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.getPlugin().getLogger());
        BungeeLibraryManager libraryManager = new BungeeLibraryManager(this.getPlugin());
        libraryManager.addRepository("https://repo.diogotc.com/mirror/");
        libraryManager.loadLibrary(Dependency.ADVENTURE.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_GSON.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_LEGACY.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_PLAIN.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_KEY.getLibrary());
        libraryManager.loadLibrary(Dependency.ADVENTURE_TEXT_SERIALIZER_JSON.getLibrary());
        libraryManager.loadLibrary(Dependency.KYORI_EXAMINATION.getLibrary());
        libraryManager.loadLibrary(Dependency.KYORI_OPTION.getLibrary());

        new BungeeTriton(this).onEnable();
    }

    @Override
    public void onDisable() {
        // Set the formatter back to default
        try {
            if (Triton.get().getConfig().isTerminal()) {
                BungeeTerminalManager.uninjectTerminalFormatter();
            }
        } catch (Error | Exception e) {
            try {
                if (Triton.get().getConfig().isTerminal()) {
                    Log4jInjector.uninjectAppender();
                }
            } catch (Error | Exception e1) {
                this.getPlugin().getLogger()
                        .log(Level.SEVERE, "Failed to uninject terminal translations. Some forked BungeeCord servers " +
                                "might not work correctly. To hide this message, disable terminal translation on " +
                                "config.");
                e.printStackTrace();
                e1.printStackTrace();
            }
        }
    }

    @Override
    public Platform getPlatform() {
        return Platform.BUNGEE;
    }

    @Override
    public TritonLogger getTritonLogger() {
        return this.logger;
    }

    @Override
    public InputStream getResourceAsStream(String fileName) {
        return this.getPlugin().getResourceAsStream(fileName);
    }

}
