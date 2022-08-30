package com.rexcantor64.triton.spigot.utils;

import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.rexcantor64.triton.utils.ComponentUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities for {@link WrappedChatComponent}.
 *
 * @since 4.0.0
 */
public class WrappedComponentUtils {

    /**
     * Convert {@link WrappedChatComponent} to a {@link Component}.
     *
     * @param component The {@link WrappedChatComponent} to convert.
     * @return The equivalent of the given {@link WrappedChatComponent}, but as a {@link Component}.
     * @since 4.0.0
     */
    public static @NotNull Component deserialize(@NotNull WrappedChatComponent component) {
        return ComponentUtils.deserializeFromJson(component.getJson());
    }

    /**
     * Convert {@link Component} to a {@link WrappedChatComponent}.
     *
     * @param component The {@link Component} to convert.
     * @return The equivalent of the given {@link Component}, but as a {@link WrappedChatComponent}.
     * @since 4.0.0
     */
    public static @NotNull WrappedChatComponent serialize(@NotNull Component component) {
        return WrappedChatComponent.fromJson(ComponentUtils.serializeToJson(component));
    }

}
