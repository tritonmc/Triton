package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.SpigotMLP;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;

public class SpigotPlugin extends JavaPlugin implements PluginLoader {

    @Override
    public void onEnable() {
        new SpigotMLP(this).onEnable();
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
