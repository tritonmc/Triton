package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.TritonLogger;
import net.byteflux.libby.LibraryManager;

import java.io.InputStream;
import java.util.Set;

public interface PluginLoader {

    Platform getPlatform();

    TritonLogger getTritonLogger();

    InputStream getResourceAsStream(String fileName);

    LibraryManager getLibraryManager();

    Set<LoaderFlag> getLoaderFlags();

    default boolean hasLoaderFlag(LoaderFlag flag) {
        return getLoaderFlags().contains(flag);
    }

    default void loadDependency(Dependency dependency) {
        getLibraryManager().loadLibrary(dependency.getLibrary(getLoaderFlags()));
    };

}
