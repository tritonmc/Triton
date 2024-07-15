package com.rexcantor64.triton.spigot.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.Converters;

import java.util.Map;
import java.util.Optional;

/**
 * Custom ProtocolLib Wrapper of NMS' Advancement
 * Does not work with versions lower than 1.20.2, but it is not used then
 * Tested only with 1.20.2
 */
public class WrappedAdvancement extends AbstractWrapper {

    private static Class<?> ADVANCEMENT = MinecraftReflection.getMinecraftClass("advancements.Advancement", "Advancement");
    private static Class<?> ADVANCEMENT_REWARDS_CLASS = MinecraftReflection.getMinecraftClass("advancements.AdvancementRewards", "AdvancementRewards");
    private static Class<?> ADVANCEMENT_REQUIREMENTS_CLASS = MinecraftReflection.getMinecraftClass("advancements.AdvancementRequirements", "AdvancementRequirements");
    private static FuzzyReflection FUZZY_REFLECTION = FuzzyReflection.fromClass(ADVANCEMENT, true);
    private static FieldAccessor MINECRAFT_KEY = Accessors.getFieldAccessor(FUZZY_REFLECTION.getParameterizedField(Optional.class, MinecraftReflection.getMinecraftKeyClass()));
    private static FieldAccessor ADVANCEMENT_DISPLAY = Accessors.getFieldAccessor(FUZZY_REFLECTION.getParameterizedField(Optional.class, WrappedAdvancementDisplay.getWrappedClass()));
    private static FieldAccessor ADVANCEMENT_REWARDS = Accessors.getFieldAccessor(ADVANCEMENT, ADVANCEMENT_REWARDS_CLASS, true);
    private static FieldAccessor CRITERION_MAP = Accessors.getFieldAccessor(ADVANCEMENT, Map.class, true);
    private static FieldAccessor ADVANCEMENT_REQUIREMENTS = Accessors.getFieldAccessor(ADVANCEMENT, ADVANCEMENT_REQUIREMENTS_CLASS, true);
    private static FieldAccessor SEND_TELEMETRY_EVENT_BOOL = Accessors.getFieldAccessor(ADVANCEMENT, boolean.class, true);
    private static FieldAccessor CHAT = Accessors.getFieldAccessor(FUZZY_REFLECTION.getParameterizedField(Optional.class, MinecraftReflection.getIChatBaseComponentClass()));
    private static ConstructorAccessor ADVANCEMENT_CONSTRUTOR = Accessors.getConstructorAccessor(
            ADVANCEMENT,
            Optional.class,
            Optional.class,
            ADVANCEMENT_REWARDS_CLASS,
            Map.class,
            ADVANCEMENT_REQUIREMENTS_CLASS,
            boolean.class,
            Optional.class
    );

    public static final EquivalentConverter<Optional<WrappedAdvancementDisplay>> DISPLAY_CONVERT = Converters.optional(WrappedAdvancementDisplay.CONVERTER);

    public static final EquivalentConverter<WrappedAdvancement> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedAdvancement::getHandle, WrappedAdvancement::fromHandle, WrappedAdvancement.class));

    /**
     * Construct a new AdvancementDisplay wrapper.
     */
    private WrappedAdvancement(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public Optional<WrappedAdvancementDisplay> getAdvancementDisplay() {
        return DISPLAY_CONVERT.getSpecific(ADVANCEMENT_DISPLAY.get(handle));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void setAdvancementDisplay(Optional<WrappedAdvancementDisplay> advancementDisplay) {
        ADVANCEMENT_DISPLAY.set(handle, DISPLAY_CONVERT.getGeneric(advancementDisplay));
    }

    public WrappedAdvancement shallowClone() {
        Object newInstance = ADVANCEMENT_CONSTRUTOR.invoke(
                MINECRAFT_KEY.get(handle),
                ADVANCEMENT_DISPLAY.get(handle),
                ADVANCEMENT_REWARDS.get(handle),
                CRITERION_MAP.get(handle),
                ADVANCEMENT_REQUIREMENTS.get(handle),
                SEND_TELEMETRY_EVENT_BOOL.get(handle),
                CHAT.get(handle)
        );

        return new WrappedAdvancement(newInstance);
    }

    /**
     * Construct a wrapped advancement display from a native NMS object.
     *
     * @param handle - the native object.
     * @return The wrapped advancement display object.
     */
    public static WrappedAdvancement fromHandle(Object handle) {
        return new WrappedAdvancement(handle);
    }

    public static Class<?> getWrappedClass() {
        return ADVANCEMENT;
    }
}
