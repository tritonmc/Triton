package com.rexcantor64.triton.api.impl;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Arrays;

@NotNullByDefault
public class ComponentUtils {

    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

    /**
     * Deserialize a {@link JsonElement} representing a {@link Component}.
     *
     * @param element The {@link JsonElement} to deserialize.
     * @return The corresponding {@link Component}.
     */
    public static Component deserializeFromJsonTree(JsonElement element) {
        return GSON_SERIALIZER.deserializeFromTree(element);
    }

    /**
     * Serialize a {@link Component} to a {@link JsonElement}.
     *
     * @param component The {@link Component} to serialize.
     * @return The corresponding {@link JsonElement}.
     */
    public static JsonElement serializeToJsonTree(Component component) {
        return GSON_SERIALIZER.serializeToTree(component);
    }

    /**
     * Deserialize an array of {@link JsonElement} representing an array of {@link Component}.
     *
     * @param elements The array of {@link JsonElement} to deserialize.
     * @return The corresponding array of {@link Component}.
     */
    public static Component[] deserializeFromJsonTree(JsonElement... elements) {
        return Arrays.stream(elements).map(ComponentUtils::deserializeFromJsonTree).toArray(Component[]::new);
    }

    /**
     * Serialize an array of {@link Component} to an array of {@link JsonElement}.
     *
     * @param components The array of {@link Component} to serialize.
     * @return The corresponding array of {@link JsonElement}.
     */
    public static JsonElement[] serializeToJsonTree(Component... components) {
        return Arrays.stream(components).map(ComponentUtils::serializeToJsonTree).toArray(JsonElement[]::new);
    }

}
