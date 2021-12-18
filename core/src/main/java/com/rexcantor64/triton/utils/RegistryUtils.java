package com.rexcantor64.triton.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import lombok.val;

public class RegistryUtils {

    public static String getTileEntityTypeKey(Object tileEntityType) {
        Class<?> iRegistry = MinecraftReflection.getIRegistry();

        // on 1.18, TileEntityType is "ad"
        Object tileEntityTypeRegistry = NMSUtils.getStaticField(iRegistry, "ad");

        val key = NMSUtils.getMethod(tileEntityTypeRegistry, "b", new Class[]{Object.class}, new Object[]{tileEntityType});
        if (key == null) {
            return null;
        }

        return MinecraftKey.fromHandle(key).getFullKey();
    }

    public static Object getTileEntityTypeFromKey(MinecraftKey key) {
        Class<?> iRegistry = MinecraftReflection.getIRegistry();

        // on 1.18, TileEntityType is "ad"
        Object tileEntityTypeRegistry = NMSUtils.getStaticField(iRegistry, "ad");

        return NMSUtils.getMethod(tileEntityTypeRegistry, "a",
                new Class[]{MinecraftReflection.getMinecraftKeyClass()}, new Object[]{MinecraftKey.getConverter().getGeneric(key)});
    }


}
