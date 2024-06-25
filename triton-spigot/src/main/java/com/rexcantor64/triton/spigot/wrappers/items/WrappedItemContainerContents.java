package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Custom ProtocolLib Wrapper of NMS' ItemContainerContents (added to NMS in 1.20.6).
 * This is used to store the items that are inside a container (e.g., shulker box).
 *
 * @since 3.10.0
 */
public class WrappedItemContainerContents extends AbstractWrapper {
    private static final Class<?> ITEM_CONTAINER_CONTENTS = MinecraftReflection.getMinecraftClass("world.item.component.ItemContainerContents");
    private static final FieldAccessor ITEMS_FIELD = Accessors.getFieldAccessor(ITEM_CONTAINER_CONTENTS, MinecraftReflection.getNonNullListClass(), true);
    private static final ConstructorAccessor CONSTRUCTOR = Accessors.getConstructorAccessor(
            FuzzyReflection.fromClass(ITEM_CONTAINER_CONTENTS, true).getConstructor(
                    FuzzyMethodContract.newBuilder()
                            .parameterExactType(List.class)
                            .build()
            )
    );

    private static final EquivalentConverter<ItemStack> ITEM_STACK_CONVERTER = BukkitConverters.getItemStackConverter();
    private static final EquivalentConverter<List<ItemStack>> ITEM_STACK_LIST_CONVERTER = BukkitConverters.getListConverter(ITEM_STACK_CONVERTER);

    private static final EquivalentConverter<WrappedItemContainerContents> CONVERTER = Converters.ignoreNull(Converters.handle(AbstractWrapper::getHandle, WrappedItemContainerContents::fromHandle, WrappedItemContainerContents.class));

    private WrappedItemContainerContents(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public @NotNull List<ItemStack> getItems() {
        return ITEM_STACK_LIST_CONVERTER.getSpecific(ITEMS_FIELD.get(this.getHandle()));
    }

    public void setItems(@NotNull List<ItemStack> items) {
        this.handle = CONSTRUCTOR.invoke(ITEM_STACK_LIST_CONVERTER.getGeneric(items));
    }

    /**
     * Construct item container contents from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped item container contents.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedItemContainerContents fromHandle(Object handle) {
        return new WrappedItemContainerContents(handle);
    }

    public static EquivalentConverter<WrappedItemContainerContents> getConverter() {
        return CONVERTER;
    }

    public static Class<?> getWrappedClass() {
        return ITEM_CONTAINER_CONTENTS;
    }
}
