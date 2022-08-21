package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.logger.TritonLogger;

import java.io.InputStream;

public interface PluginLoader {

    Platform getPlatform();

    TritonLogger getTritonLogger();

    InputStream getResourceAsStream(String fileName);

}
