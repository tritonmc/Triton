package com.rexcantor64.triton.wrappers.legacy;

import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import lombok.val;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Optional;

public class HoverComponentWrapper {

    private static final MethodAccessor NBT_DESERIALIZER_METHOD;

    static {
        val mojangsonParserClass = MinecraftReflection.getMinecraftClass("nbt.MojangsonParser", "MojangsonParser");
        FuzzyReflection fuzzy = FuzzyReflection.fromClass(mojangsonParserClass);
        val method = fuzzy.getMethodByParameters("deserializeNbtCompound", MinecraftReflection.getNBTCompoundClass(), new Class<?>[]{String.class});
        NBT_DESERIALIZER_METHOD = Accessors.getMethodAccessor(method);
    }

    public static BaseComponent[] getValue(HoverEvent hover) {
        return hover.getValue();
    }

    public static HoverEvent setValue(HoverEvent hover, BaseComponent... components) {
        return new HoverEvent(hover.getAction(), components);
    }

    private static NbtCompound deserializeItemTagNbt(String nbt) {
        val nmsCompound = NBT_DESERIALIZER_METHOD.invoke(null, nbt);
        return NbtFactory.fromNMSCompound(nmsCompound);
    }

    private static String serializeItemTagNbt(NbtCompound nbt) {
        return nbt.getHandle().toString();
    }

    /**
     * Get an NBT compound with the item data, stored in a hover event.
     * If the item does not have a "tag" attribute, an empty Optional is returned,
     * since it does not have any metadata to translate.
     *
     * @param components The text stored inside the hover event.
     * @return If valid, the NBT compound in the base component.
     */
    public static Optional<NbtCompound> getNbtItemData(BaseComponent[] components) {
        if (components.length != 1) {
            return Optional.empty();
        }
        if (!(components[0] instanceof TextComponent)) {
            return Optional.empty();
        }
        val component = (TextComponent) components[0];
        val itemMojangson = component.getText();

        val itemCompound = deserializeItemTagNbt(itemMojangson);
        if (!itemCompound.containsKey("tag")) {
            return Optional.empty();
        }
        //return Optional.of(itemCompound.getCompoundOrDefault("tag"));
        return Optional.of(itemCompound);
    }

    /**
     * Get a chat component that has a serialized Mojangson NBT item data string inside.
     *
     * @param compound The NBT item data to serialize.
     * @return The chat component with the NBT item data inside.
     */
    public static BaseComponent[] fromNbtItemData(NbtCompound compound) {
        val serializedNbt = serializeItemTagNbt(compound);

        return new BaseComponent[]{new TextComponent(serializedNbt)};
    }

}
