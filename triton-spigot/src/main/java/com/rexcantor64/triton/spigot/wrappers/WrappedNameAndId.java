package com.rexcantor64.triton.spigot.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom ProtocolLib Wrapper of NMS' NameAndId (added to NMS in 1.21.9)
 * Tested with 1.21.10
 */
public class WrappedNameAndId extends AbstractWrapper {

    private static final @Nullable Class<?> NAME_AND_ID_CLASS;
    private static ConstructorAccessor CONSTRUCTOR;
    private static FieldAccessor UUID_FIELD;
    private static FieldAccessor NAME_FIELD;

    public static final EquivalentConverter<WrappedNameAndId> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedNameAndId::getHandle, WrappedNameAndId::fromHandle, WrappedNameAndId.class));
    public static final EquivalentConverter<List<WrappedNameAndId>> LIST_CONVERTER = BukkitConverters.getListConverter(CONVERTER);

    static {
        NAME_AND_ID_CLASS = MinecraftReflection.getNullableNMS("server.players.NameAndId");
        if (NAME_AND_ID_CLASS != null) {
            CONSTRUCTOR = Accessors.getConstructorAccessor(NAME_AND_ID_CLASS, UUID.class, String.class);
            UUID_FIELD = Accessors.getFieldAccessor(NAME_AND_ID_CLASS, UUID.class, true);
            NAME_FIELD = Accessors.getFieldAccessor(NAME_AND_ID_CLASS, String.class, true);
        }
    }

    private WrappedNameAndId(Object handle) {
        super(Objects.requireNonNull(getWrappedClass(), "NameAndId class does not exist on this version"));
        setHandle(handle);
    }

    public WrappedNameAndId(UUID id, String name) {
        this(CONSTRUCTOR.invoke(id, name));
    }

    public UUID getUniqueId() {
        return (UUID) UUID_FIELD.get(handle);
    }

    public String getName() {
        return (String) NAME_FIELD.get(handle);
    }

    @Contract("_ -> new")
    public WrappedNameAndId withUniqueId(UUID id) {
        return new WrappedNameAndId(id, this.getName());
    }

    @Contract("_ -> new")
    public WrappedNameAndId withName(String name) {
        return new WrappedNameAndId(this.getUniqueId(), name);
    }

    @Contract("_ -> new")
    public static @NotNull WrappedNameAndId fromHandle(Object handle) {
        return new WrappedNameAndId(handle);
    }

    public static @Nullable Class<?> getWrappedClass() {
        return NAME_AND_ID_CLASS;
    }
}
