package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.MinecraftKey;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedRegistrable;
import com.comphenix.protocol.wrappers.WrappedRegistry;
import lombok.val;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Optional;

/**
 * Custom ProtocolLib Wrapper of NMS' PatchedDataComponentMap (added to NMS in 1.20.6).
 * This is used to store metadata of items.
 *
 * @since 3.10.0
 */
public class WrappedPatchedDataComponentMap extends AbstractWrapper {
    private static final Class<?> DATA_COMPONENT_TYPE = MinecraftReflection.getMinecraftClass("core.component.DataComponentType");
    private static final Class<?> DATA_COMPONENT_MAP = MinecraftReflection.getMinecraftClass("core.component.DataComponentMap");
    private static final Class<?> PATCHED_DATA_COMPONENT_MAP = MinecraftReflection.getMinecraftClass("core.component.PatchedDataComponentMap");
    private static final FieldAccessor ITEM_DATA_COMPONENT_MAP_FIELD = Accessors.getFieldAccessor(MinecraftReflection.getItemStackClass(), PATCHED_DATA_COMPONENT_MAP, true);

    private static final MethodAccessor GET_METHOD = Accessors.getMethodAccessor(
            // Get this method from interface to avoid collision with "remove" method
            FuzzyReflection
                    .fromClass(DATA_COMPONENT_MAP)
                    .getMethodByReturnTypeAndParameters("get", Object.class, DATA_COMPONENT_TYPE)
    );
    private static final MethodAccessor SET_METHOD = Accessors.getMethodAccessor(
            FuzzyReflection
                    .fromClass(PATCHED_DATA_COMPONENT_MAP)
                    .getMethodByReturnTypeAndParameters("set", Object.class, DATA_COMPONENT_TYPE, Object.class)
    );

    private static final WrappedRegistrable TYPE_CUSTOM_NAME;
    private static final WrappedRegistrable TYPE_LORE;
    private static final WrappedRegistrable TYPE_CONTAINER;
    private static final WrappedRegistrable TYPE_WRITTEN_BOOK_CONTENT;

    static {
        // Workaround for https://github.com/dmulloy2/ProtocolLib/issues/3027
        // In 1.21, there are two registries of type DataComponentType, so WrappedRegistry returns the incorrect one.
        if (MinecraftVersion.v1_21_0.atOrAbove()) {
            MethodAccessor registryResourceKeyMethod = Accessors.getMethodAccessor(
                    FuzzyReflection.fromClass(MinecraftReflection.getIRegistry())
                            .getMethodByReturnTypeAndParameters("key", MinecraftReflection.getResourceKey())
            );
            Object registry = Arrays.stream(Accessors.getFieldAccessorArray(MinecraftReflection.getBuiltInRegistries(), MinecraftReflection.getIRegistry(), false))
                    .map(registryField -> registryField.get(null))
                    .filter(registryObj -> {
                        val resourceKey = registryResourceKeyMethod.invoke(registryObj);
                        val modifier = new StructureModifier<MinecraftKey>(resourceKey.getClass())
                                .withTarget(resourceKey)
                                .withType(MinecraftReflection.getMinecraftKeyClass(), MinecraftKey.getConverter());
                        MinecraftKey registryKey = modifier.read(0);
                        MinecraftKey locationKey = modifier.read(1);
                        return registryKey.getFullKey().equals("minecraft:root") && locationKey.getFullKey().equals("minecraft:data_component_type");
                    })
                    .findAny()
                    .orElseThrow(() -> new RuntimeException("cannot find data component type registry for item translation"));

            // Ironically, use ProtocolLib on ProtocolLib
            ConstructorAccessor wrappedRegistryConstructor = Accessors.getConstructorAccessor(WrappedRegistry.class, Object.class);
            WrappedRegistry wrappedRegistry = (WrappedRegistry) wrappedRegistryConstructor.invoke(registry);

            // The WrappedRegistrable will have an incorrect factory, but that's fine because we only use the handle anyway
            TYPE_CUSTOM_NAME = WrappedRegistrable.fromHandle(DATA_COMPONENT_TYPE, wrappedRegistry.get("custom_name"));
            TYPE_LORE = WrappedRegistrable.fromHandle(DATA_COMPONENT_TYPE, wrappedRegistry.get("lore"));
            TYPE_CONTAINER = WrappedRegistrable.fromHandle(DATA_COMPONENT_TYPE, wrappedRegistry.get("container"));
            TYPE_WRITTEN_BOOK_CONTENT = WrappedRegistrable.fromHandle(DATA_COMPONENT_TYPE, wrappedRegistry.get("written_book_content"));
        } else {
            TYPE_CUSTOM_NAME = WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, "custom_name");
            TYPE_LORE = WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, "lore");
            TYPE_CONTAINER = WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, "container");
            TYPE_WRITTEN_BOOK_CONTENT = WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, "written_book_content");
        }
    }

    private WrappedPatchedDataComponentMap(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public @Nullable Object get(@NotNull String key) {
        return get(WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, key));
    }

    public @Nullable Object get(@NotNull WrappedRegistrable key) {
        return GET_METHOD.invoke(this.getHandle(), key.getHandle());
    }

    public @Nullable WrappedChatComponent getCustomName() {
        return BukkitConverters.getWrappedChatComponentConverter().getSpecific(get(TYPE_CUSTOM_NAME));
    }

    public @Nullable WrappedItemLore getLore() {
        return WrappedItemLore.getConverter().getSpecific(get(TYPE_LORE));
    }

    public @Nullable WrappedWrittenBookContent getWrittenBookContent() {
        return WrappedWrittenBookContent.getConverter().getSpecific(get(TYPE_WRITTEN_BOOK_CONTENT));
    }

    public @Nullable WrappedItemContainerContents getContainer() {
        return WrappedItemContainerContents.getConverter().getSpecific(get(TYPE_CONTAINER));
    }

    public void set(@NotNull String key, @Nullable Object value) {
        set(WrappedRegistrable.fromClassAndKey(DATA_COMPONENT_TYPE, key), value);
    }

    public @Nullable Object set(@NotNull WrappedRegistrable key, @Nullable Object value) {
        return SET_METHOD.invoke(this.getHandle(), key.getHandle(), value);
    }

    public void setCustomName(@Nullable WrappedChatComponent component) {
        set(TYPE_CUSTOM_NAME, BukkitConverters.getWrappedChatComponentConverter().getGeneric(component));
    }

    public void setLore(@Nullable WrappedItemLore lore) {
        set(TYPE_LORE, WrappedItemLore.getConverter().getGeneric(lore));
    }

    public void setWrittenBookContent(@Nullable WrappedWrittenBookContent writtenBookContent) {
        set(TYPE_WRITTEN_BOOK_CONTENT, WrappedWrittenBookContent.getConverter().getGeneric(writtenBookContent));
    }

    public void setContainer(@Nullable WrappedItemContainerContents itemContainerContents) {
        set(TYPE_CONTAINER, WrappedItemContainerContents.getConverter().getGeneric(itemContainerContents));
    }

    /**
     * Construct a patched data component map from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped data component map.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedPatchedDataComponentMap fromHandle(Object handle) {
        return new WrappedPatchedDataComponentMap(handle);
    }

    /**
     * Get the underlying patched data component map of a craft item stack.
     *
     * @param item - the item stack to get the data component map from.
     * @return The wrapped data component map.
     */
    @Contract("_ -> new")
    public static @NotNull Optional<WrappedPatchedDataComponentMap> fromItemStack(@Nullable ItemStack item) {
        if (item == null || item.getType().isAir() || !MinecraftReflection.isCraftItemStack(item)) {
            return Optional.empty();
        }
        Object itemHandle = MinecraftReflection.getMinecraftItemStack(item);
        return Optional.of(fromHandle(ITEM_DATA_COMPONENT_MAP_FIELD.get(itemHandle)));
    }

    public static Class<?> getWrappedClass() {
        return PATCHED_DATA_COMPONENT_MAP;
    }
}
