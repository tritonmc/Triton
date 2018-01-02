package com.rexcantor64.multilanguageplugin.plugin;

import com.rexcantor64.multilanguageplugin.BungeeMLP;
import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin implements PluginLoader {

    @Override
    public void onEnable() {
        new BungeeMLP(this).onEnable();
    }

    @Override
    public PluginType getType() {
        return PluginType.BUNGEE;
    }

    @Override
    public SpigotPlugin asSpigot() {
        return null;
    }

    @Override
    public BungeePlugin asBungee() {
        return this;
    }
}
