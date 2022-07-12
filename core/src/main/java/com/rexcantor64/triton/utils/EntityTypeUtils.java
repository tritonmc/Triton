package com.rexcantor64.triton.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.wrappers.EntityType;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

public class EntityTypeUtils {

    private static Method registryGetTypeByNumericIdMethod = null;
    private static Method registryGetMinecraftKeyByTypeMethod = null;
    private static Object entityTypeRegistry = null;
    private static final HashMap<Integer, EntityType> cache = new HashMap<>();

    public static EntityType getEntityTypeById(int id) {
        if (!cache.containsKey(id))
            cache.put(id, getEntityTypeByIdNoCache(id));
        return cache.get(id);
    }

    private static EntityType getEntityTypeByIdNoCache(int id) {
        try {
            if (Triton.get().getMcVersion() >= 13) {
                calculateEntityRegistrySet();

                Object type = registryGetTypeByNumericIdMethod.invoke(entityTypeRegistry, id);
                Object minecraftKey = registryGetMinecraftKeyByTypeMethod.invoke(entityTypeRegistry, type);
                return EntityType.fromBukkit(org.bukkit.entity.EntityType.fromName(minecraftKey.toString()
                        .replace("minecraft:", "")));
            }
            return EntityType.fromBukkit(org.bukkit.entity.EntityType.fromId(id));
        } catch (Exception e) {
            Triton.get().getLogger().logError(e, "Failed to get the EntityType from the type id. Is the plugin up to date?");
        }
        return EntityType.UNKNOWN;
    }

    public static EntityType getEntityTypeByObjectId(int id) {
        for (ObjectIds obj : ObjectIds.values())
            if (obj.id == id) return obj.entityType;
        return EntityType.UNKNOWN;
    }

    @SneakyThrows
    private static void calculateEntityRegistrySet() {
        if (EntityTypeUtils.entityTypeRegistry != null) return;

        Class<?> iRegistry = MinecraftReflection.getIRegistry();
        Class<?> minecraftKeyClass = MinecraftReflection.getMinecraftKeyClass();
        Class<?> registryBlocksClass = MinecraftReflection.getMinecraftClass("core.RegistryBlocks", "RegistryBlocks");

        Object entityTypeRegistry = Arrays.stream(iRegistry.getFields())
                .filter(field -> {
                    // 1.13-1.16 uses IRegistry as type, 1.17+ uses RegistryBlocks as type
                    if (field.getType().equals(registryBlocksClass) || field.getType().equals(iRegistry)) {
                        ParameterizedType type = (ParameterizedType) field.getGenericType();
                        Type[] actualTypes = type.getActualTypeArguments();
                        return actualTypes.length == 1 && actualTypes[0].getTypeName().endsWith(".EntityTypes<?>");
                    }
                    return false;
                })
                .findAny()
                .orElseThrow(() -> new RuntimeException("Could not get EntityTypes registry. Incompatible Minecraft version."))
                .get(null);

        registryGetMinecraftKeyByTypeMethod = Arrays.stream(entityTypeRegistry.getClass().getMethods())
                .filter(method -> minecraftKeyClass.equals(method.getReturnType()) && method.getParameterCount() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not get RegistryBlocks<EntityType>'s key set"));

        registryGetTypeByNumericIdMethod = Arrays.stream(entityTypeRegistry.getClass().getMethods())
                .filter(method -> method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(int.class))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not get entity numeric id to entity type method"));

        EntityTypeUtils.entityTypeRegistry = entityTypeRegistry;

    }

    private enum ObjectIds {
        BOAT(1, EntityType.BOAT),
        ITEM_STACK(2, EntityType.DROPPED_ITEM),
        AREA_EFFECT_CLOUD(3, EntityType.AREA_EFFECT_CLOUD),
        MINECART(10, EntityType.MINECART),
        TNT(50, EntityType.PRIMED_TNT),
        ENDER_CRISTAL(51, EntityType.ENDER_CRYSTAL),
        ARROW(60, EntityType.ARROW),
        SNOWBALL(61, EntityType.SNOWBALL),
        EGG(62, EntityType.EGG),
        FIREBALL(63, EntityType.FIREBALL),
        FIRECHARGE(64, EntityType.SMALL_FIREBALL),
        ENDERPEARL(65, EntityType.ENDER_PEARL),
        WITHER_SKULL(66, EntityType.WITHER_SKULL),
        SHULKER_BULLET(67, EntityType.SHULKER_BULLET),
        LLAMA_SPIT(68, EntityType.LLAMA_SPIT),
        FALLING_OBJECTS(70, EntityType.FALLING_BLOCK),
        ITEM_FRAME(71, EntityType.ITEM_FRAME),
        EYE_OF_ENDER(72, EntityType.ENDER_SIGNAL),
        POTION(73, EntityType.SPLASH_POTION),
        EXP_BOTTLE(75, EntityType.THROWN_EXP_BOTTLE),
        FIREWORK(76, EntityType.FIREWORK),
        LEASH(77, EntityType.LEASH_HITCH),
        ARMOR_STAND(78, EntityType.ARMOR_STAND),
        EVOCATION_FANGS(79, EntityType.EVOKER_FANGS),
        FISHING_HOOK(90, EntityType.FISHING_HOOK),
        SPECTRAL_ARROW(91, EntityType.SPECTRAL_ARROW),
        DRAGON_FIREBALL(93, EntityType.DRAGON_FIREBALL),
        TRIDENT(94, EntityType.TRIDENT);
        int id;
        EntityType entityType;

        ObjectIds(int id, EntityType entityType) {
            this.id = id;
            this.entityType = entityType;
        }
    }

}
