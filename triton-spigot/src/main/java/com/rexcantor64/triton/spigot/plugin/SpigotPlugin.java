package com.rexcantor64.triton.spigot.plugin;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.logger.JavaLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.terminal.Log4jInjector;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;

public class SpigotPlugin extends JavaPlugin implements PluginLoader {
    private TritonLogger logger;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.getLogger());
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
        return getResource(fileName);
    }

    @Override
    public TritonLogger getTritonLogger() {
        return this.logger;
    }
}
