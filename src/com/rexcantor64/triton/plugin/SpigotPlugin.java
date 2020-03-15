package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;

public class SpigotPlugin extends JavaPlugin implements PluginLoader {

    @Override
    public void onEnable() {
        new SpigotMLP(this).onEnable();
    }

    @Override
    public void onDisable() {
        if (Triton.get().getConf().isTerminal()) {
            Logger logger = (Logger) LogManager.getRootLogger();
            Configuration config = logger.getContext().getConfiguration();
            if (logger.getAppenders().containsKey("TritonTerminalTranslation")) {
                logger.removeAppender(logger.getAppenders().get("TritonTerminalTranslation"));
                logger.addAppender(config.getAppenders().get("TerminalConsole"));
            }
        }
    }

    @Override
    public PluginType getType() {
        return PluginType.SPIGOT;
    }

    @Override
    public SpigotPlugin asSpigot() {
        return this;
    }

    @Override
    public BungeePlugin asBungee() {
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String fileName) {
        return getResource(fileName);
    }

    @Override
    public void shutdown() {
        Bukkit.getPluginManager().disablePlugin(this);
    }
}
