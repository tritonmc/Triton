package com.rexcantor64.triton.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

/**
 * Custom ProtocolLib Wrapper of NMS' AdvancementDisplay
 */
public class WrappedAdvancementDisplay extends AbstractWrapper {

    private static Class<?> ADVANCEMENT_DISPLAY = MinecraftReflection.getMinecraftClass("advancements.AdvancementDisplay", "AdvancementDisplay");
    private static Class<?> ADVANCEMENT_FRAME_TYPE_CLASS = MinecraftReflection.getMinecraftClass("advancements.AdvancementFrameType", "AdvancementFrameType");
    private static ConstructorAccessor ADVANCEMENT_DISPLAY_CONSTRUTOR = Accessors.getConstructorAccessor(
            ADVANCEMENT_DISPLAY,
            MinecraftReflection.getItemStackClass(),
            MinecraftReflection.getIChatBaseComponentClass(),
            MinecraftReflection.getIChatBaseComponentClass(),
            MinecraftReflection.getMinecraftKeyClass(),
            ADVANCEMENT_FRAME_TYPE_CLASS,
            boolean.class,
            boolean.class,
            boolean.class);
    private static FieldAccessor[] CHAT_COMPONENTS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, MinecraftReflection.getIChatBaseComponentClass(), true);
    private static FieldAccessor TITLE = CHAT_COMPONENTS[0];
    private static FieldAccessor DESCRIPTION = CHAT_COMPONENTS[1];
    private static FieldAccessor ITEM_STACK = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, MinecraftReflection.getItemStackClass(), true);
    private static FieldAccessor MINECRAFT_KEY = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, MinecraftReflection.getMinecraftKeyClass(), true);
    private static FieldAccessor ADVANCEMENT_FRAME_TYPE = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, ADVANCEMENT_FRAME_TYPE_CLASS, true);
    private static FieldAccessor[] BOOLEANS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, boolean.class, true);
    private static FieldAccessor SHOW_TOAST = BOOLEANS[0];
    private static FieldAccessor ANNOUNCE_TO_CHAT = BOOLEANS[1];
    private static FieldAccessor HIDDEN = BOOLEANS[2];
    private static FieldAccessor[] FLOATS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, float.class, true);
    private static FieldAccessor X_CORD = FLOATS[0];
    private static FieldAccessor Y_CORD = FLOATS[1];

    private static EquivalentConverter<WrappedChatComponent> CHAT_CONVERT = BukkitConverters.getWrappedChatComponentConverter();

    /**
     * Construct a new AdvancementDisplay wrapper.
     */
    private WrappedAdvancementDisplay(Object handle) {
        super(getWrappedClass());
        setHandle(handle);
    }

    public WrappedChatComponent getTitle() {
        return CHAT_CONVERT.getSpecific(TITLE.get(handle));
    }

    public WrappedChatComponent getDescription() {
        return CHAT_CONVERT.getSpecific(DESCRIPTION.get(handle));
    }

    public void setTitle(WrappedChatComponent title) {
        TITLE.set(handle, CHAT_CONVERT.getGeneric(title));
    }

    public void setDescription(WrappedChatComponent description) {
        DESCRIPTION.set(handle, CHAT_CONVERT.getGeneric(description));
    }

    public WrappedAdvancementDisplay shallowClone() {
        Object newInstance = ADVANCEMENT_DISPLAY_CONSTRUTOR.invoke(
            ITEM_STACK.get(handle),
            TITLE.get(handle),
            DESCRIPTION.get(handle),
            MINECRAFT_KEY.get(handle),
            ADVANCEMENT_FRAME_TYPE.get(handle),
            SHOW_TOAST.get(handle),
            ANNOUNCE_TO_CHAT.get(handle),
            HIDDEN.get(handle)
        );
        X_CORD.set(newInstance, X_CORD.get(handle));
        Y_CORD.set(newInstance, Y_CORD.get(handle));

        return new WrappedAdvancementDisplay(newInstance);
    }

    /**
     * Construct a wrapped advancement display from a native NMS object.
     * @param handle - the native object.
     * @return The wrapped advancement display object.
     */
    public static WrappedAdvancementDisplay fromHandle(Object handle) {
        return new WrappedAdvancementDisplay(handle);
    }

    public static Class<?> getWrappedClass() {
        return ADVANCEMENT_DISPLAY;
    }
}
