package com.rexcantor64.triton.terminal;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.log.ColouredWriter;
import net.md_5.bungee.log.ConciseFormatter;

import java.util.logging.Handler;

public class BungeeTerminalManager {

    public static void injectTerminalFormatter() {
        for (Handler h : BungeeCord.getInstance().getLogger().getHandlers())
            if (h instanceof ColouredWriter)
                h.setFormatter(new BungeeTerminalFormatter());
    }

    public static void uninjectTerminalFormatter() {
        for (Handler h : BungeeCord.getInstance().getLogger().getHandlers())
            if (h instanceof ColouredWriter)
                h.setFormatter(new ConciseFormatter());
    }

}
