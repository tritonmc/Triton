package com.rexcantor64.triton.spigot.wrappers.items;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.Converters;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Custom ProtocolLib Wrapper of NMS' Filterable (added to NMS in 1.20.6).
 * This is used while storing written book's content.
 *
 * @since 3.10.0
 */
@Getter
public class WrappedFilterable<T> extends AbstractWrapper {
    private static final Class<?> ITEM_LORE = MinecraftReflection.getMinecraftClass("server.network.Filterable");
    private static final FieldAccessor RAW_FIELD = Accessors.getFieldAccessor(ITEM_LORE, Object.class, true);
    private static final FieldAccessor FILTERED_FIELD = Accessors.getFieldAccessor(ITEM_LORE, Optional.class, true);
    private static final ConstructorAccessor CONSTRUCTOR = Accessors.getConstructorAccessor(ITEM_LORE, Object.class, Optional.class);

    private final EquivalentConverter<T> innerConverter;

    private WrappedFilterable(Object handle, EquivalentConverter<T> innerConverter) {
        super(getWrappedClass());
        setHandle(handle);
        this.innerConverter = innerConverter;
    }

    public @NotNull T getRaw() {
        return this.innerConverter.getSpecific(RAW_FIELD.get(this.getHandle()));
    }

    public void setRaw(@NotNull T inner) {
        setHandle(CONSTRUCTOR.invoke(
                this.innerConverter.getGeneric(inner),
                FILTERED_FIELD.get(getHandle())
        ));
    }

    /**
     * Construct a filterable from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped filterable.
     */
    @Contract("_, _ -> new")
    public static <T> @NotNull WrappedFilterable<T> fromHandle(Object handle, EquivalentConverter<T> innerConverter) {
        return new WrappedFilterable<T>(handle, innerConverter);
    }

    public static <T> EquivalentConverter<WrappedFilterable<T>> getConverter(EquivalentConverter<T> innerConverter) {
        return Converters.ignoreNull(new EquivalentConverter<WrappedFilterable<T>>() {
            @Override
            public Object getGeneric(WrappedFilterable<T> wrappedFilterable) {
                return wrappedFilterable.getHandle();
            }

            @Override
            public WrappedFilterable<T> getSpecific(Object handle) {
                return WrappedFilterable.fromHandle(handle, innerConverter);
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<WrappedFilterable<T>> getSpecificType() {
                // Damn you Java
                Class<?> dummy = WrappedFilterable.class;
                return (Class<WrappedFilterable<T>>) dummy;
            }
        });
    }

    public static Class<?> getWrappedClass() {
        return ITEM_LORE;
    }
}
