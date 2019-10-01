package com.rexcantor64.triton.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.wrappers.EntityType;

import java.util.HashMap;
import java.util.Set;

public class EntityTypeUtils {

    private static short MAJOR_VERSION = -1;
    private static HashMap<Integer, EntityType> cache = new HashMap<>();

    static {
        MAJOR_VERSION = Short.parseShort(MinecraftReflection.getPackageVersion().split("_")[1]);
    }

    public static EntityType getEntityTypeById(int id) {
        if (!cache.containsKey(id))
            cache.put(id, getEntityTypeByIdNoCache(id));
        return cache.get(id);
    }

    private static EntityType getEntityTypeByIdNoCache(int id) {
        try {
            if (MAJOR_VERSION >= 13) {
                Class<?> registryClass = MinecraftReflection.getMinecraftClass("IRegistry");
                Object entityTypeFinder = registryClass.getField("ENTITY_TYPE").get(null);
                Set<?> entitySet = (Set<?>) entityTypeFinder.getClass().getMethod("keySet").invoke(entityTypeFinder);
                return EntityType.fromBukkit(org.bukkit.entity.EntityType.fromName(entitySet.toArray()[id].toString()
                        .replace("minecraft:", "")));
            }
            return EntityType.fromBukkit(org.bukkit.entity.EntityType.fromId(id));
        } catch (Exception e) {
            if (Triton.get().getConf().isDebug())
                e.printStackTrace();
        }
        return EntityType.UNKNOWN;
    }

}
