package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.logger.JavaLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.terminal.Log4jInjector;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;

public class SpigotPlugin extends JavaPlugin implements PluginLoader {
    private TritonLogger logger;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.getLogger());
        new SpigotMLP(this).onEnable();
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
