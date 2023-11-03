package com.rexcantor64.triton.spigot.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.Converters;

/**
 * Custom ProtocolLib Wrapper of NMS' ClientConfiguration.
 * Added to NMS in 1.20.2.
 */
public class WrappedClientConfiguration extends AbstractWrapper {

    private static Class<?> CLIENT_INFORMATION = MinecraftReflection.getMinecraftClass("server.level.ClientInformation");
    private static FieldAccessor LOCALE = Accessors.getFieldAccessor(CLIENT_INFORMATION, String.class, true);

    public static final EquivalentConverter<WrappedClientConfiguration> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedClientConfiguration::getHandle, WrappedClientConfiguration::fromHandle, WrappedClientConfiguration.class));

    /**
     * Construct a new ClientConfiguration wrapper.
     */
    private WrappedClientConfiguration(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public String getLocale() {
        return (String) LOCALE.get(handle);
    }

    /**
     * Construct a wrapped advancement display from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped advancement display object.
     */
    public static WrappedClientConfiguration fromHandle(Object handle) {
        return new WrappedClientConfiguration(handle);
    }

    public static Class<?> getWrappedClass() {
        return CLIENT_INFORMATION;
    }
}
