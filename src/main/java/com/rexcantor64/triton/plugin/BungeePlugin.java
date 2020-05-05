package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.terminal.Log4jInjector;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Level;

public class BungeePlugin extends Plugin implements PluginLoader {

    @Override
    public void onEnable() {
        new BungeeMLP(this).onEnable();
    }

    @Override
    public void onDisable() {
        // Set the formatter back to default
        try {
            if (Triton.get().getConf().isTerminal())
                BungeeTerminalManager.uninjectTerminalFormatter();
        } catch (Error | Exception e) {
            try {
                if (Triton.get().getConf().isTerminal())
                    Log4jInjector.uninjectAppender();
            } catch (Error | Exception ignored) {
                getLogger()
                        .log(Level.SEVERE, "Failed to uninject terminal translations. Some forked BungeeCord servers " +
                                "might not work correctly. To hide this message, disable terminal translation on " +
                                "config.");
            }
        }
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
