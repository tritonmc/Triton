package com.rexcantor64.triton.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Custom ProtocolLib Wrapper of NMS' PlayerChatMessage (added to NMS in 1.19 and removed in 1.19.3)
 * Tested with 1.19.2
 */
public class WrappedPlayerChatMessage extends AbstractWrapper {

    private static Class<?> PLAYER_CHAT_MESSAGE = MinecraftReflection.getMinecraftClass("network.chat.PlayerChatMessage");
    private static FuzzyReflection FUZZY_REFLECTION = FuzzyReflection.fromClass(PLAYER_CHAT_MESSAGE, true);
    private static FieldAccessor CHAT_COMPONENT = Accessors.getFieldAccessor(FUZZY_REFLECTION.getParameterizedField(Optional.class, MinecraftReflection.getIChatBaseComponentClass()));
    private static EquivalentConverter<Optional<WrappedChatComponent>> CHAT_COMPONENT_CONVERTER = Converters.optional(BukkitConverters.getWrappedChatComponentConverter());

    public static final EquivalentConverter<WrappedPlayerChatMessage> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedPlayerChatMessage::getHandle, WrappedPlayerChatMessage::fromHandle, WrappedPlayerChatMessage.class));

    /**
     * Construct a new PlayerChatMessage wrapper.
     */
    private WrappedPlayerChatMessage(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public Optional<WrappedChatComponent> getMessage() {
        return CHAT_COMPONENT_CONVERTER.getSpecific(CHAT_COMPONENT.get(handle));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void setMessage(Optional<WrappedChatComponent> message) {
        CHAT_COMPONENT.set(handle, CHAT_COMPONENT_CONVERTER.getGeneric(message));
    }

    /**
     * Construct a player chat message from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped player chat message object.
     */
    @Contract("_ -> new")
    public static @NotNull WrappedPlayerChatMessage fromHandle(Object handle) {
        return new WrappedPlayerChatMessage(handle);
    }

    public static Class<?> getWrappedClass() {
        return PLAYER_CHAT_MESSAGE;
    }
}
