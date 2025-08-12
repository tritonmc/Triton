package com.rexcantor64.triton.utils;

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.VersionedComponentSerializer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
@RequiredArgsConstructor
public class VersionedComponentUtils {

    private final @NotNull VersionedComponentSerializer serializer;

    public BaseComponent[] parse(String json) {
        return serializer.parse(json);
    }

    public BaseComponent deserialize(String json) {
        return serializer.deserialize(json);
    }

    public BaseComponent deserialize(JsonElement json) {
        return serializer.deserialize(json);
    }

    public String toString(BaseComponent... component) {
        return serializer.toString(component);
    }

    public JsonElement toJson(BaseComponent component) {
        return serializer.toJson(component);
    }

    public BaseComponent[] reparse(BaseComponent... component) {
        return serializer.parse(serializer.toString(component));
    }
}
