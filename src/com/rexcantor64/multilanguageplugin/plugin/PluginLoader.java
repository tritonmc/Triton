package com.rexcantor64.multilanguageplugin.plugin;

import java.io.InputStream;
import java.util.logging.Logger;

public interface PluginLoader {

    PluginType getType();

    SpigotPlugin asSpigot();

    BungeePlugin asBungee();

    Logger getLogger();

    InputStream getResourceAsStream(String fileName);

    enum PluginType {
        SPIGOT, BUNGEE
    }

}
