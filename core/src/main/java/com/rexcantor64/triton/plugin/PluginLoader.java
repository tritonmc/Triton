package com.rexcantor64.triton.plugin;

import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.dependencies.DependencyManager;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.TritonLogger;
import lombok.val;

import java.io.InputStream;
import java.util.Set;

public interface PluginLoader {

    Platform getPlatform();

    TritonLogger getTritonLogger();

    InputStream getResourceAsStream(String fileName);

    DependencyManager getDependencyManager();

    Set<LoaderFlag> getLoaderFlags();

    /**
     * Load all adventure-related libraries according to the enabled loader flags.
     *
     * @since 4.0.0
     */
    default void loadAdventure() {
        val depManager = getDependencyManager();
        if (depManager.hasLoaderFlag(LoaderFlag.VENDOR_ADVENTURE)) {
            depManager.loadDependency(Dependency.ADVENTURE);
            depManager.loadDependency(Dependency.ADVENTURE_KEY);
            depManager.loadDependency(Dependency.KYORI_EXAMINATION_API);
            depManager.loadDependency(Dependency.KYORI_EXAMINATION_STRING);
        }
        if (depManager.hasLoaderFlag(LoaderFlag.VENDOR_ADVENTURE_NBT)) {
            depManager.loadDependency(Dependency.ADVENTURE_NBT);
        }
        if (depManager.hasLoaderFlag(LoaderFlag.VENDOR_ADVENTURE_SERIALIZERS)) {
            depManager.loadDependency(Dependency.KYORI_OPTION);
            depManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_GSON);
            depManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_JSON);
            depManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_LEGACY);
            depManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_PLAIN);
            depManager.loadDependency(Dependency.ADVENTURE_MINI_MESSAGE);
        }
        if (depManager.hasLoaderFlag(LoaderFlag.VENDOR_ADVENTURE_BUNGEE_SERIALIZER)) {
            depManager.loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_BUNGEECORD);
        }
    }
}
