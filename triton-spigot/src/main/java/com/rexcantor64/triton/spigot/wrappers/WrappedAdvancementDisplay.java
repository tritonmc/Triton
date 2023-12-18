package com.rexcantor64.triton.spigot.wrappers;

import com.comphenix.protocol.reflect.EquivalentConverter;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.ConstructorAccessor;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.comphenix.protocol.wrappers.AbstractWrapper;
import com.comphenix.protocol.wrappers.BukkitConverters;
import com.comphenix.protocol.wrappers.Converters;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import java.util.Optional;

/**
 * Custom ProtocolLib Wrapper of NMS' AdvancementDisplay
 */
public class WrappedAdvancementDisplay extends AbstractWrapper {

    private static Class<?> ADVANCEMENT_DISPLAY = MinecraftReflection.getMinecraftClass("advancements.AdvancementDisplay", "AdvancementDisplay");
    private static Class<?> ADVANCEMENT_FRAME_TYPE_CLASS = MinecraftReflection.getMinecraftClass("advancements.AdvancementFrameType", "AdvancementFrameType");
    private static ConstructorAccessor ADVANCEMENT_DISPLAY_CONSTRUTOR = Accessors.getConstructorAccessor(
            ADVANCEMENT_DISPLAY,
            MinecraftReflection.getItemStackClass(), // icon
            MinecraftReflection.getIChatBaseComponentClass(), // title
            MinecraftReflection.getIChatBaseComponentClass(), // description
            MinecraftVersion.v1_20_4.atOrAbove() ? Optional.class : MinecraftReflection.getMinecraftKeyClass(), // background
            ADVANCEMENT_FRAME_TYPE_CLASS, // frame
            boolean.class, // showToast
            boolean.class, // announceToChat
            boolean.class // hidden
    );
    private static FieldAccessor[] CHAT_COMPONENTS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, MinecraftReflection.getIChatBaseComponentClass(), true);
    private static FieldAccessor TITLE = CHAT_COMPONENTS[0];
    private static FieldAccessor DESCRIPTION = CHAT_COMPONENTS[1];
    private static FieldAccessor ITEM_STACK = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, MinecraftReflection.getItemStackClass(), true);
    private static FieldAccessor MINECRAFT_KEY;
    private static FieldAccessor ADVANCEMENT_FRAME_TYPE = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, ADVANCEMENT_FRAME_TYPE_CLASS, true);
    private static FieldAccessor[] BOOLEANS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, boolean.class, true);
    private static FieldAccessor SHOW_TOAST = BOOLEANS[0];
    private static FieldAccessor ANNOUNCE_TO_CHAT = BOOLEANS[1];
    private static FieldAccessor HIDDEN = BOOLEANS[2];
    private static FieldAccessor[] FLOATS = Accessors.getFieldAccessorArray(ADVANCEMENT_DISPLAY, float.class, true);
    private static FieldAccessor X_CORD = FLOATS[0];
    private static FieldAccessor Y_CORD = FLOATS[1];

    private static EquivalentConverter<WrappedChatComponent> CHAT_CONVERT = BukkitConverters.getWrappedChatComponentConverter();

    public static final EquivalentConverter<WrappedAdvancementDisplay> CONVERTER = Converters.ignoreNull(Converters.handle(WrappedAdvancementDisplay::getHandle, WrappedAdvancementDisplay::fromHandle, WrappedAdvancementDisplay.class));

    private boolean hasChanged = false;

    static {
        if (MinecraftVersion.v1_20_4.atOrAbove()) {
            FuzzyReflection fuzzyReflection = FuzzyReflection.fromClass(ADVANCEMENT_DISPLAY, true);
            MINECRAFT_KEY = Accessors.getFieldAccessor(fuzzyReflection.getParameterizedField(Optional.class, MinecraftReflection.getMinecraftKeyClass()));
        } else {
            MINECRAFT_KEY = Accessors.getFieldAccessor(ADVANCEMENT_DISPLAY, MinecraftReflection.getMinecraftKeyClass(), true);
        }
    }

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
        hasChanged = true;
    }

    public void setDescription(WrappedChatComponent description) {
        DESCRIPTION.set(handle, CHAT_CONVERT.getGeneric(description));
        hasChanged = true;
    }

    public boolean hasChangedAndReset() {
        boolean changed = hasChanged;
        hasChanged = false;
        return changed;
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
     *
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
