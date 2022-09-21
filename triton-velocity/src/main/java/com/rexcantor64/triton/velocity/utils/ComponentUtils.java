package com.rexcantor64.triton.velocity.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.util.VelocityLegacyHoverEventSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class ComponentUtils extends com.rexcantor64.triton.utils.ComponentUtils {
    private static final GsonComponentSerializer PRE_1_16_SERIALIZER = GsonComponentSerializer.builder()
            .downsampleColors()
            .emitLegacyHoverEvent()
            .legacyHoverEventSerializer(VelocityLegacyHoverEventSerializer.INSTANCE)
            .build();
    private static final GsonComponentSerializer MODERN_SERIALIZER = GsonComponentSerializer.builder()
            .legacyHoverEventSerializer(VelocityLegacyHoverEventSerializer.INSTANCE)
            .build();

    /**
     * Deserialize a JSON string representing a {@link Component}.
     *
     * @param json The JSON to deserialize.
     * @param protocolVersion The version of the client receiving sending this
     * @return The corresponding {@link Component}.
     */
    public static Component deserializeFromJson(@NotNull String json, @NotNull ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
            return MODERN_SERIALIZER.deserialize(json);
        } else {
            return PRE_1_16_SERIALIZER.deserialize(json);
        }
    }

    /**
     * Serialize a {@link Component} to a JSON string.
     *
     * @param component The {@link Component} to serialize.
     * @return The corresponding JSON string.
     */
    public static String serializeToJson(@NotNull Component component, @NotNull ProtocolVersion protocolVersion) {
        if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
            return MODERN_SERIALIZER.serialize(component);
        } else {
            return PRE_1_16_SERIALIZER.serialize(component);
        }
    }

}
