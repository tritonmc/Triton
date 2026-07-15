package com.rexcantor64.triton.spigot.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.jetbrains.annotations.NotNull;

/**
 * Utilities for {@link BaseComponent BaseComponents}.
 *
 * @since 4.0.0
 */
public class BaseComponentUtils {

    /**
     * Convert {@link BaseComponent} to a {@link Component}.
     *
     * @param components The {@link BaseComponent} to convert.
     * @return The equivalent of the given {@link BaseComponent}, but as a {@link Component}.
     * @since 4.0.0
     */
    public static @NotNull Component deserialize(@NotNull BaseComponent... components) {
        return GsonComponentSerializer.gson().deserialize(ComponentSerializer.toString(components));
    }

    /**
     * Convert {@link Component} to a {@link BaseComponent}.
     *
     * @param component The {@link Component} to convert.
     * @return The equivalent of the given {@link Component}, but as a {@link BaseComponent}.
     * @since 4.0.0
     */
    public static @NotNull BaseComponent @NotNull [] serialize(@NotNull Component component) {
        return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component));
    }

}
