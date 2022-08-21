package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.BungeeMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.logger.JavaLogger;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.terminal.BungeeTerminalManager;
import com.rexcantor64.triton.terminal.Log4jInjector;
import net.md_5.bungee.api.plugin.Plugin;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;

import java.util.logging.Level;

public class BungeePlugin extends Plugin implements PluginLoader {
    private TritonLogger logger;

    @Override
    public void onEnable() {
        this.logger = new JavaLogger(this.getLogger());
        new BungeeMLP(this).onEnable();
    }

    @Override
    public void onDisable() {
        // Set the formatter back to default
        try {
            if (Triton.get().getConfig().isTerminal())
                BungeeTerminalManager.uninjectTerminalFormatter();
        } catch (Error | Exception e) {
            try {
                if (Triton.get().getConfig().isTerminal())
                    Log4jInjector.uninjectAppender();
            } catch (Error | Exception e1) {
                getLogger()
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

}
