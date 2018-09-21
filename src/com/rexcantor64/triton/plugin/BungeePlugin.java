package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.BungeeMLP;
import net.md_5.bungee.BungeeCord;
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

    @Override
    public void shutdown() {
        onDisable();
        BungeeCord.getInstance().getPluginManager().unregisterCommands(this);
        BungeeCord.getInstance().getPluginManager().unregisterListeners(this);
        BungeeCord.getInstance().unregisterChannel("MultiLanguagePlugin");
    }
}
