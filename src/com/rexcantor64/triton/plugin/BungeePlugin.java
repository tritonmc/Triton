package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.Triton;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.log.ColouredWriter;
import net.md_5.bungee.log.ConciseFormatter;

import java.util.logging.Handler;

public class BungeePlugin extends Plugin implements PluginLoader {

    @Override
    public void onEnable() {
        new BungeeMLP(this).onEnable();
    }

    @Override
    public void onDisable() {
        // Set the formatter back to default
        if (Triton.get().getConf().isTerminal())
            for (Handler h : BungeeCord.getInstance().getLogger().getHandlers())
                if (h instanceof ColouredWriter)
                    h.setFormatter(new ConciseFormatter());
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
        BungeeCord.getInstance().unregisterChannel("triton:main");
    }
}
