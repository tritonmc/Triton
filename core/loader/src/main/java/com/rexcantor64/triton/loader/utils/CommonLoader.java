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
}
