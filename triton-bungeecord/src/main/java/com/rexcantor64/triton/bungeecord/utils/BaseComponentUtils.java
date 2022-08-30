package com.rexcantor64.triton.bungeecord.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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
        return BungeeComponentSerializer.get().deserialize(components);
    }

    /**
     * Convert {@link Component} to a {@link BaseComponent}.
     *
     * @param component The {@link Component} to convert.
     * @return The equivalent of the given {@link Component}, but as a {@link BaseComponent}.
     * @since 4.0.0
     */
    public static @NotNull BaseComponent @NotNull [] serialize(@NotNull Component component) {
        return BungeeComponentSerializer.get().serialize(component);
    }


    /**
     * Wrap an array of {@link BaseComponent} in a since {@link BaseComponent}.
     * If the given array only has one element, that element is returned.
     *
     * @param components The array to wrap in a single {@link BaseComponent}.
     * @return The {@link BaseComponent} with the given array as extras.
     * @since 4.0.0
     */
    public static BaseComponent convertArrayToSingle(BaseComponent... components) {
        if (components.length == 1) {
            return components[0];
        }
        BaseComponent result = new TextComponent("");
        result.setExtra(Arrays.asList(components));
        return result;
    }

}
