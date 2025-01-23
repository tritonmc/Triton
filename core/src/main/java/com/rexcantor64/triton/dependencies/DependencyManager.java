package com.rexcantor64.triton.dependencies;

import com.rexcantor64.triton.loader.utils.LoaderFlag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.byteflux.libby.LibraryManager;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@RequiredArgsConstructor
public class DependencyManager {
    private final LibraryManager libraryManager;
    private final Set<LoaderFlag> loaderFlags;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public boolean hasLoaderFlag(LoaderFlag flag) {
        return getLoaderFlags().contains(flag);
    }

    public void loadDependency(Dependency dependency) {
        getLibraryManager().loadLibrary(dependency.getLibrary(getLoaderFlags()));
    }

    public synchronized void init() {
        if (initialized.getAndSet(true)) {
            return;
        }

        libraryManager.addRepository(Repository.DIOGOTC_MIRROR);
        libraryManager.addMavenCentral();

        if (hasLoaderFlag(LoaderFlag.SHADE_ADVENTURE)) {
            if (hasLoaderFlag(LoaderFlag.RELOCATE_ADVENTURE)) {
                loadDependency(Dependency.ADVENTURE);
                loadDependency(Dependency.ADVENTURE_KEY);
                loadDependency(Dependency.KYORI_EXAMINATION_API);
                loadDependency(Dependency.KYORI_EXAMINATION_STRING);
            }
            loadDependency(Dependency.KYORI_OPTION);
            loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_GSON);
            loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_JSON);
            loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_LEGACY);
            loadDependency(Dependency.ADVENTURE_TEXT_SERIALIZER_PLAIN);
            loadDependency(Dependency.ADVENTURE_MINI_MESSAGE);
        }
    }
}
