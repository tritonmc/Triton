package com.rexcantor64.triton.spigot.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.Converters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Custom ProtocolLib Wrapper of NMS' AdvancementHolder (added to NMS in 1.20.2)
 * Tested with 1.20.2
 */
public class WrappedAdvancementHolder extends AbstractWrapper {

    private static Class<?> ADVANCEMENT_HOLDER = MinecraftReflection.getMinecraftClass("advancements.AdvancementHolder");
    private static Class<?> ADVANCEMENT_CLASS = MinecraftReflection.getMinecraftClass("advancements.Advancement", "Advancement");
    private static FieldAccessor ADVANCEMENT = Accessors.getFieldAccessor(ADVANCEMENT_HOLDER, ADVANCEMENT_CLASS, true);

    public static final EquivalentConverter<WrappedAdvancementHolder> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedAdvancementHolder::getHandle, WrappedAdvancementHolder::fromHandle, WrappedAdvancementHolder.class));

    /**
     * Construct a new AdvancementDisplay wrapper.
     */
    private WrappedAdvancementHolder(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public WrappedAdvancement getAdvancement() {
        return WrappedAdvancement.CONVERTER.getSpecific(ADVANCEMENT.get(handle));
    }

    public void setAdvancement(WrappedAdvancement advancement) {
        ADVANCEMENT.set(handle, WrappedAdvancement.CONVERTER.getGeneric(advancement));
    }

    /**
     * Construct a wrapped advancement display from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped advancement display object.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedAdvancementHolder fromHandle(Object handle) {
        return new WrappedAdvancementHolder(handle);
    }

    public static Class<?> getWrappedClass() {
        return ADVANCEMENT_HOLDER;
    }
}
