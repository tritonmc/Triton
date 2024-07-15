package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.dependencies.DependencyManager;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.TritonLogger;

import java.io.InputStream;
import java.util.Set;

public interface PluginLoader {

    Platform getPlatform();

    TritonLogger getTritonLogger();

    InputStream getResourceAsStream(String fileName);

    DependencyManager getDependencyManager();

    Set<LoaderFlag> getLoaderFlags();

}
