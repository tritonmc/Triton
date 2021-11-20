package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.logger.TritonLogger;

import java.io.InputStream;

public interface PluginLoader {

    PluginType getType();

    TritonLogger getTritonLogger();

    InputStream getResourceAsStream(String fileName);

    enum PluginType {
        SPIGOT, BUNGEE, VELOCITY
    }

}
