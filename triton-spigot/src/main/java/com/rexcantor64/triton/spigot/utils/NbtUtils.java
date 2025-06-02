package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;
import com.comphenix.protocol.wrappers.nbt.NbtList;
import com.comphenix.protocol.wrappers.nbt.NbtType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities for handling NBT tags.
 *
 * @since 3.12.0
 */
public class NbtUtils {

    /**
     * Convert ProtocolLib's {@link NbtBase} to a {@link JsonElement}.
     * Inspired by <a href="https://github.com/SpigotMC/BungeeCord/blob/9cd0d3289f33c8a77170fe99bf69090858c9ddea/protocol/src/main/java/net/md_5/bungee/protocol/TagUtil.java#L165">BungeeCord's TagUtil</a>
     *
     * @param tag The NBT tag.
     * @return The equivalent tag, as a JSON tree.
     * @since 3.12.0
     */
    public static @NotNull JsonElement toJson(@NotNull NbtBase<?> tag) {
        switch (tag.getType()) {
            case TAG_BYTE:
                return new JsonPrimitive(((NbtBase<Byte>) tag).getValue());
            case TAG_SHORT:
                return new JsonPrimitive(((NbtBase<Short>) tag).getValue());
            case TAG_INT:
                return new JsonPrimitive(((NbtBase<Integer>) tag).getValue());
            case TAG_LONG:
                return new JsonPrimitive(((NbtBase<Long>) tag).getValue());
            case TAG_FLOAT:
                return new JsonPrimitive(((NbtBase<Float>) tag).getValue());
            case TAG_DOUBLE:
                return new JsonPrimitive(((NbtBase<Double>) tag).getValue());
            case TAG_BYTE_ARRAY:
                Byte[] byteArray = ((NbtBase<Byte[]>) tag).getValue();

                JsonArray jsonByteArray = new JsonArray(byteArray.length);
                for (byte b : byteArray) {
                    jsonByteArray.add(new JsonPrimitive(b));
                }
                return jsonByteArray;
            case TAG_INT_ARRAY:
                Integer[] intArray = ((NbtBase<Integer[]>) tag).getValue();

                JsonArray jsonIntArray = new JsonArray(intArray.length);
                for (int i : intArray) {
                    jsonIntArray.add(new JsonPrimitive(i));
                }
                return jsonIntArray;
            case TAG_LONG_ARRAY:
                Long[] longArray = ((NbtBase<Long[]>) tag).getValue();

                JsonArray jsonLongArray = new JsonArray(longArray.length);
                for (long l : longArray) {
                    jsonLongArray.add(new JsonPrimitive(l));
                }
                return jsonLongArray;
            case TAG_STRING:
                return new JsonPrimitive(((NbtBase<String>) tag).getValue());
            case TAG_LIST:
                List<? extends NbtBase<?>> nbtBaseList = ((NbtList<?>) tag).getValue();

                JsonArray jsonList = new JsonArray(nbtBaseList.size());
                for (NbtBase<?> nbtBase : nbtBaseList) {
                    jsonList.add(toJson(nbtBase));
                }
                return jsonList;
            case TAG_COMPOUND:
                JsonObject jsonObject = new JsonObject();
                for (val entry : (NbtCompound) tag) {
                    jsonObject.add(entry.getName(), toJson(entry));
                }
                return jsonObject;
            default:
                throw new IllegalArgumentException("Unknown NBT tag type: " + tag.getType());
        }
    }

    /**
     * Convert a {@link JsonElement} to ProtocolLib's {@link NbtBase}.
     * Inspired by <a href="https://github.com/SpigotMC/BungeeCord/blob/9cd0d3289f33c8a77170fe99bf69090858c9ddea/protocol/src/main/java/net/md_5/bungee/protocol/TagUtil.java#L35">BungeeCord's TagUtil</a>
     *
     * @param element The JSON element.
     * @return The equivalent element, as an NBT tag.
     * @since 3.12.0
     */
    public static @NotNull NbtBase<?> fromJson(@NotNull JsonElement element) {
        if (element.isJsonPrimitive()) {
            val primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                val number = primitive.getAsNumber();
                if (number instanceof Byte) {
                    return NbtFactory.of(null, (byte) number);
                } else if (number instanceof Short) {
                    return NbtFactory.of(null, (short) number);
                } else if (number instanceof Integer) {
                    return NbtFactory.of(null, (int) number);
                } else if (number instanceof Long) {
                    return NbtFactory.of(null, (long) number);
                } else if (number instanceof Float) {
                    return NbtFactory.of(null, (float) number);
                } else if (number instanceof Double) {
                    return NbtFactory.of(null, (double) number);
                }
            } else if (primitive.isString()) {
                return NbtFactory.of(null, primitive.getAsString());
            } else if (primitive.isBoolean()) {
                return NbtFactory.of(null, (byte) (primitive.getAsBoolean() ? 1 : 0));
            }
        } else if (element.isJsonObject()) {
            val object = element.getAsJsonObject();
            val compound = NbtFactory.ofCompound(null);
            for (val entry : object.entrySet()) {
                val value = fromJson(entry.getValue());
                value.setName(entry.getKey());
                compound.put(value);
            }
            return compound;
        } else if (element.isJsonArray()) {
            val jsonArray = element.getAsJsonArray();
            val list = jsonArray.asList().stream()
                    .map(NbtUtils::fromJson)
                    .peek(e -> e.setName(""))
                    .collect(Collectors.toList());

            NbtType listType = null;
            for (val nbtBase : list) {
                // If all element have same type, use that type. Otherwise, it's a list of compounds
                val type = nbtBase.getType();
                if (listType == null) {
                    listType = type;
                } else if (listType != type) {
                    listType = NbtType.TAG_COMPOUND;
                    break;
                }
            }

            if (listType == NbtType.TAG_BYTE) {
                byte[] bytes = new byte[jsonArray.size()];
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = jsonArray.get(i).getAsByte();
                }
                return NbtFactory.of(null, bytes);
            } else if (listType == NbtType.TAG_INT) {
                int[] ints = new int[jsonArray.size()];
                for (int i = 0; i < ints.length; i++) {
                    ints[i] = jsonArray.get(i).getAsInt();
                }
                return NbtFactory.of(null, ints);
            } else if (listType == NbtType.TAG_LONG) {
                long[] longs = new long[jsonArray.size()];
                for (int i = 0; i < longs.length; i++) {
                    longs[i] = jsonArray.get(i).getAsLong();
                }
                return NbtFactory.ofWrapper(NbtType.TAG_LONG_ARRAY, null, longs);
            } else {
                return NbtFactory.ofList(null, list);
            }
        }
        throw new IllegalArgumentException("Unable to convert JSON element to NBT tag: " + element);
    }
}
