package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.rexcantor64.triton.utils.ReflectionUtils;
import lombok.SneakyThrows;
import lombok.val;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class RegistryUtils {

    private static Object tileEntityTypeRegistry = null;

    public static String getTileEntityTypeKey(Object tileEntityType) {
        calculateTileEntityTypeRegistry();

        val key = ReflectionUtils.getMethod(tileEntityTypeRegistry, "b", new Class[]{Object.class}, new Object[]{tileEntityType});
        if (key == null) {
            return null;
        }

        return MinecraftKey.fromHandle(key).getFullKey();
    }

    public static Object getTileEntityTypeFromKey(MinecraftKey key) {
        calculateTileEntityTypeRegistry();

        return ReflectionUtils.getMethod(tileEntityTypeRegistry, "a",
                new Class[]{MinecraftReflection.getMinecraftKeyClass()}, new Object[]{MinecraftKey.getConverter().getGeneric(key)});
    }

    @SneakyThrows
    private static void calculateTileEntityTypeRegistry() {
        if (tileEntityTypeRegistry != null) return;

        Class<?> iRegistry = MinecraftReflection.getIRegistry();

        tileEntityTypeRegistry = Arrays.stream(iRegistry.getFields())
                .filter(field -> {
                    if (field.getType().equals(iRegistry)) {
                        ParameterizedType type = (ParameterizedType) field.getGenericType();
                        Type[] actualTypes = type.getActualTypeArguments();
                        return actualTypes.length == 1 && actualTypes[0].getTypeName().equals("net.minecraft.world.level.block.entity.TileEntityTypes<?>");
                    }
                    return false;
                })
                .findAny()
                .orElseThrow(() -> new RuntimeException("Could not get TileEntityType registry. Incompatible Minecraft version."))
                .get(null);
    }


}
