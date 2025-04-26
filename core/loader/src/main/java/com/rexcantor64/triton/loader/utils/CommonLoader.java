package com.rexcantor64.triton.loader.utils;

import lombok.Builder;
import lombok.Singular;
import lombok.val;
import me.lucko.jarrelocator.Relocation;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Builder
public class CommonLoader {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";

    private final String jarInJarName;
    private final String bootstrapClassName;
    @Singular
    private Set<LoaderFlag> flags;
    @Singular
    private final List<Class<?>> constructorTypes;
    @Singular
    private final List<Object> constructorValues;

    public LoaderBootstrap loadPlugin() {
        List<Relocation> relocations = new ArrayList<>();
        if (flags.contains(LoaderFlag.SHADE_ADVENTURE)) {
            if (flags.contains(LoaderFlag.RELOCATE_ADVENTURE)) {
                relocations.add(new Relocation("net/kyori/adventure", "com/rexcantor64/triton/lib/adventure"));
            } else {
                // relocate only specific adventure libraries (instead of everything)
                relocations.add(new Relocation("net/kyori/adventure/text/minimessage", "com/rexcantor64/triton/lib/adventure/text/minimessage"));
                relocations.add(new Relocation("net/kyori/adventure/text/serializer/gson", "com/rexcantor64/triton/lib/adventure/text/serializer/gson"));
                relocations.add(new Relocation("net/kyori/adventure/text/serializer/legacy", "com/rexcantor64/triton/lib/adventure/text/serializer/legacy"));
                relocations.add(new Relocation("net/kyori/adventure/text/serializer/plain", "com/rexcantor64/triton/lib/adventure/text/serializer/plain"));
                relocations.add(new Relocation("net/kyori/adventure/text/serializer/bungeecord", "com/rexcantor64/triton/lib/adventure/text/serializer/bungeecord"));
            }
        }

        if (flags.contains(LoaderFlag.VENDOR_PACKET_EVENTS)) {
            relocations.add(new Relocation("com/github/retrooper/packetevents", "com/rexcantor64/triton/lib/packetevents/api"));
            relocations.add(new Relocation("io/github/retrooper/packetevents", "com/rexcantor64/triton/lib/packetevents/impl"));
        }

        @SuppressWarnings("resource")
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), relocations, CORE_JAR_NAME, jarInJarName);

        Class<?>[] constructorTypes = this.constructorTypes.toArray(new Class<?>[this.constructorTypes.size() + 1]);
        constructorTypes[constructorTypes.length - 1] = Set.class;
        Object[] constructorValues = this.constructorValues.toArray(new Object[this.constructorValues.size() + 1]);
        constructorValues[constructorValues.length - 1] = Collections.unmodifiableSet(this.flags);
        return loader.instantiatePlugin(bootstrapClassName, constructorTypes, constructorValues);
    }

    /**
     * Override loader flags with configuration in loader.conf.
     * Server administrators are encouraged to not override loader flags, since it can cause Triton
     * to not work correctly.
     *
     * @param pluginDirectory The configuration directory for Triton, where to look for loader.conf.
     * @return This instance of {@link CommonLoader}.
     * @since 4.0.0
     */
    @Contract("_ -> this")
    public CommonLoader loadUserLoaderFlags(Path pluginDirectory) {
        val loaderConfig = pluginDirectory.resolve("loader.conf");
        if (!loaderConfig.toFile().isFile()) {
            return this;
        }
        val newFlags = new HashSet<>(this.flags);
        try (Stream<String> stream = Files.lines(loaderConfig)) {
            stream
                    .map(String::trim)
                    .filter(s -> !s.startsWith("#"))
                    .map(String::toUpperCase)
                    .forEach(s -> {
                        val invert = s.startsWith("!");
                        if (invert) {
                            s = s.substring(1);
                        }
                        try {
                            val flag = LoaderFlag.valueOf(s);
                            if (invert) {
                                newFlags.remove(flag);
                                System.out.println("Forcefully disabled loader flag '" + s + "'");
                            } else {
                                newFlags.add(flag);
                                System.out.println("Forcefully enabled loader flag '" + s + "'");
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Loader flag '" + s + "' does not exist, ignored");
                        }
                    });
        } catch (IOException e) {
            System.out.println("Failed to load user flags from loader.conf");
            e.printStackTrace();
        }
        this.flags = Collections.unmodifiableSet(newFlags);
        return this;
    }
}
