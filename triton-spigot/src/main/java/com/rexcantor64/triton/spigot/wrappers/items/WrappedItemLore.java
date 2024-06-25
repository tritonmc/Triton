package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Custom ProtocolLib Wrapper of NMS' ItemLore (added to NMS in 1.20.6).
 * This is used to store the lore of items.
 *
 * @since 3.10.0
 */
public class WrappedItemLore extends AbstractWrapper {
    private static final Class<?> ITEM_LORE = MinecraftReflection.getMinecraftClass("world.item.component.ItemLore");
    private static final FieldAccessor LINES_FIELD = Accessors.getFieldAccessor(ITEM_LORE, List.class, true);
    private static final ConstructorAccessor CONSTRUCTOR = Accessors.getConstructorAccessor(ITEM_LORE, List.class);

    private static final EquivalentConverter<List<WrappedChatComponent>> COMPONENT_LIST_CONVERTER = BukkitConverters.getListConverter(BukkitConverters.getWrappedChatComponentConverter());

    private static final EquivalentConverter<WrappedItemLore> CONVERTER = Converters.ignoreNull(Converters.handle(AbstractWrapper::getHandle, WrappedItemLore::fromHandle, WrappedItemLore.class));

    private WrappedItemLore(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public @NotNull List<WrappedChatComponent> getLines() {
        return COMPONENT_LIST_CONVERTER.getSpecific(LINES_FIELD.get(this.getHandle()));
    }

    public void setLines(@NotNull List<WrappedChatComponent> lines) {
        setHandle(CONSTRUCTOR.invoke(COMPONENT_LIST_CONVERTER.getGeneric(lines)));
    }

    /**
     * Construct an item lore from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped item lore.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedItemLore fromHandle(Object handle) {
        return new WrappedItemLore(handle);
    }

    public static EquivalentConverter<WrappedItemLore> getConverter() {
        return CONVERTER;
    }

    public static Class<?> getWrappedClass() {
        return ITEM_LORE;
    }
}
