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
        libraryManager.setRelocator(new NativeRelocationHelper());
    }
}
