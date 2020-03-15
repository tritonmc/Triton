package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.terminal.Log4jInjector;
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
        if (Triton.get().getConf().isTerminal())
            Log4jInjector.uninjectAppender();
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
