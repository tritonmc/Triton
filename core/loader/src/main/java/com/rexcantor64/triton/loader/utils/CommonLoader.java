package com.rexcantor64.triton.loader.utils;

import lombok.Builder;
import lombok.Singular;
import me.lucko.jarrelocator.Relocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Builder
public class CommonLoader {
    private static final String CORE_JAR_NAME = "triton-core.jarinjar";

    private final String jarInJarName;
    private final String bootstrapClassName;
    @Singular
    private final Set<LoaderFlag> flags;
    @Singular
    private final List<Class<?>> constructorTypes;
    @Singular
    private final List<Object> constructorValues;

    public LoaderBootstrap loadPlugin() {
        List<Relocation> relocations = new ArrayList<>();
        if (flags.contains(LoaderFlag.RELOCATE_ADVENTURE)) {
            relocations.add(new Relocation("net/kyori/adventure", "com/rexcantor64/triton/lib/adventure"));
        }

        @SuppressWarnings("resource")
        JarInJarClassLoader loader = new JarInJarClassLoader(getClass().getClassLoader(), relocations, CORE_JAR_NAME, jarInJarName);

        Class<?>[] constructorTypes = this.constructorTypes.toArray(new Class<?>[this.constructorTypes.size() + 1]);
        constructorTypes[constructorTypes.length - 1] = Set.class;
        Object[] constructorValues = this.constructorValues.toArray(new Object[this.constructorValues.size() + 1]);
        constructorValues[constructorValues.length - 1] = Collections.unmodifiableSet(this.flags);
        return loader.instantiatePlugin(bootstrapClassName, constructorTypes, constructorValues);
    }
}
