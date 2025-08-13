package com.rexcantor64.triton.utils;

import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.chat.VersionedComponentSerializer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface VersionedComponentUtils {

    BaseComponent[] parse(String json);

    BaseComponent deserialize(String json);

    BaseComponent deserialize(JsonElement json);

    String toString(BaseComponent... component);

    JsonElement toJson(BaseComponent component);

    BaseComponent[] reparse(BaseComponent... component);

    @RequiredArgsConstructor
    class Versioned implements VersionedComponentUtils {
        private final @NotNull VersionedComponentSerializer serializer;

        @Override
        public BaseComponent[] parse(String json) {
            return serializer.parse(json);
        }

        @Override
        public BaseComponent deserialize(String json) {
            return serializer.deserialize(json);
        }

        @Override
        public BaseComponent deserialize(JsonElement json) {
            return serializer.deserialize(json);
        }

        @Override
        public String toString(BaseComponent... component) {
            return serializer.toString(component);
        }

        @Override
        public JsonElement toJson(BaseComponent component) {
            return serializer.toJson(component);
        }

        @Override
        public BaseComponent[] reparse(BaseComponent... component) {
            return serializer.parse(serializer.toString(component));
        }
    }

    class Legacy implements VersionedComponentUtils {

        @Override
        public BaseComponent[] parse(String json) {
            return ComponentSerializer.parse(json);
        }

        @Override
        public BaseComponent deserialize(String json) {
            return ComponentSerializer.deserialize(json);
        }

        @Override
        public BaseComponent deserialize(JsonElement json) {
            return ComponentSerializer.deserialize(json);
        }

        @Override
        public String toString(BaseComponent... component) {
            return ComponentSerializer.toString(component);
        }

        @Override
        public JsonElement toJson(BaseComponent component) {
            return ComponentSerializer.toJson(component);
        }

        @Override
        public BaseComponent[] reparse(BaseComponent... component) {
            return ComponentSerializer.parse(ComponentSerializer.toString(component));
        }
    }
}
