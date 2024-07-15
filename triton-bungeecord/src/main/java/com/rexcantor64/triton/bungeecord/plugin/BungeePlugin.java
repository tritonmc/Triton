package com.rexcantor64.triton.bungeecord.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.bungeecord.BungeeTriton;
import com.rexcantor64.triton.bungeecord.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.dependencies.DependencyManager;
import com.rexcantor64.triton.loader.utils.LoaderBootstrap;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
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
import java.util.Set;
import java.util.logging.Level;

@RequiredArgsConstructor
public class BungeePlugin implements PluginLoader, LoaderBootstrap {
    private TritonLogger logger;
    @Getter
    private final Plugin plugin;
    @Getter
    private final Set<LoaderFlag> loaderFlags;
    @Getter
    private DependencyManager dependencyManager;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.getPlugin().getLogger());
        this.dependencyManager = new DependencyManager(new BungeeLibraryManager(this.getPlugin()), loaderFlags);

        this.dependencyManager.init();
        this.dependencyManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD);

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
        return getClass().getClassLoader().getResourceAsStream(fileName);
    }

}
