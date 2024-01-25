package com.rexcantor64.triton.loader.utils;

import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Classloader that can load a jar from within another jar file.
 *
 * <p>The "loader" jar contains the loading code & public API classes,
 * and is class-loaded by the platform.</p>
 *
 * <p>The inner "plugin" jar contains the plugin itself, and is class-loaded
 * by the loading code & this classloader.</p>
 */
public class JarInJarClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Creates a new jar-in-jar class loader.
     *
     * @param loaderClassLoader the loader plugin's classloader (setup and created by the platform)
     * @param jarResourcePaths  one or more paths to the jar-in-jar resources within the loader jar
     * @throws LoadingException if something unexpectedly bad happens
     */
    public JarInJarClassLoader(ClassLoader loaderClassLoader, List<Relocation> relocations, String... jarResourcePaths) throws LoadingException {
        super(
                Arrays.stream(jarResourcePaths)
                        .map(path -> extractJar(loaderClassLoader, path, relocations))
                        .toArray(URL[]::new),
                loaderClassLoader);
    }

    public void addJarToClasspath(URL url) {
        addURL(url);
    }

    public void deleteJarResource() {
        URL[] urls = getURLs();
        if (urls.length == 0) {
            return;
        }

        try {
            Path path = Paths.get(urls[0].toURI());
            Files.deleteIfExists(path);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Creates a new plugin instance.
     *
     * @param bootstrapClass   the name of the bootstrap plugin class
     * @param loaderPluginType the type of the loader plugin, the only parameter of the bootstrap
     *                         plugin constructor
     * @param loaderPlugin     the loader plugin instance
     * @param <T>              the type of the loader plugin
     * @return the instantiated bootstrap plugin
     */
    public <T> LoaderBootstrap instantiatePlugin(String bootstrapClass, Class<T> loaderPluginType, T loaderPlugin) throws LoadingException {
        return this.instantiatePlugin(bootstrapClass, new Class[]{loaderPluginType}, new Object[]{loaderPlugin});
    }

    /**
     * Creates a new plugin instance.
     *
     * @param bootstrapClass   the name of the bootstrap plugin class
     * @param loaderPluginType the type of the loader plugin, the only parameter of the bootstrap
     *                         plugin constructor
     * @param loaderPlugin     the loader plugin instance
     * @return the instantiated bootstrap plugin
     */
    public LoaderBootstrap instantiatePlugin(String bootstrapClass, Class<?>[] loaderPluginType, Object[] loaderPlugin) throws LoadingException {
        Class<? extends LoaderBootstrap> plugin;
        try {
            plugin = loadClass(bootstrapClass).asSubclass(LoaderBootstrap.class);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to load bootstrap class", e);
        }

        Constructor<? extends LoaderBootstrap> constructor;
        try {
            constructor = plugin.getConstructor(loaderPluginType);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to get bootstrap constructor", e);
        }

        try {
            return constructor.newInstance(loaderPlugin);
        } catch (ReflectiveOperationException e) {
            throw new LoadingException("Unable to create bootstrap plugin instance", e);
        }
    }

    /**
     * Extracts the "jar-in-jar" from the loader plugin into a temporary file,
     * then returns a URL that can be used by the {@link JarInJarClassLoader}.
     *
     * @param loaderClassLoader the classloader for the "host" loader plugin
     * @param jarResourcePath   the inner jar resource path
     * @return a URL to the extracted file
     */
    private static URL extractJar(ClassLoader loaderClassLoader, String jarResourcePath, List<Relocation> relocations) throws LoadingException {
        // get the jar-in-jar resource
        URL jarInJar = loaderClassLoader.getResource(jarResourcePath);
        if (jarInJar == null) {
            throw new LoadingException("Could not locate jar-in-jar");
        }

        // create a temporary file
        // on posix systems by default this is only read/writable by the process owner
        Path path;
        Path pathRelocated;
        try {
            path = Files.createTempFile(jarResourcePath.replace('.', '-'), ".jar.tmp");
            pathRelocated = Files.createTempFile(jarResourcePath.replace('.', '-'), "-relocated.jar.tmp");
        } catch (IOException e) {
            throw new LoadingException("Unable to create a temporary file", e);
        }

        // mark that the file should be deleted on exit
        path.toFile().deleteOnExit();
        pathRelocated.toFile().deleteOnExit();

        // copy the jar-in-jar to the temporary file path
        try (InputStream in = jarInJar.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new LoadingException("Unable to copy jar-in-jar to temporary path", e);
        }

        if (!relocations.isEmpty()) {
            try {
                new JarRelocator(path.toFile(), pathRelocated.toFile(), relocations).run();
            } catch (IOException e) {
                throw new LoadingException("Unable to apply relocations to jar", e);
            }
        }

        try {
            if (relocations.isEmpty()) {
                return path.toUri().toURL();
            } else {
                return pathRelocated.toUri().toURL();
            }
        } catch (MalformedURLException e) {
            throw new LoadingException("Unable to get URL from path", e);
        }
    }

}
