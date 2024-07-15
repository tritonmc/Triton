package com.rexcantor64.triton.bungeecord.utils;

import com.google.gson.Gson;
import com.rexcantor64.triton.Triton;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Utilities for {@link BaseComponent BaseComponents}.
 *
 * @since 4.0.0
 */
public class BaseComponentUtils {

    static {
        // I hate this
        // https://github.com/KyoriPowered/adventure-platform/blob/36ab6311d9023a4f67d2db6a2e057d9ee3f8a8a7/platform-bungeecord/src/main/java/net/kyori/adventure/platform/bungeecord/BungeeAudiencesImpl.java#L63-L70
        try {
            final Field gsonField = ProxyServer.getInstance().getClass().getDeclaredField("gson");
            gsonField.setAccessible(true);
            final Gson gson = (Gson) gsonField.get(ProxyServer.getInstance());
            BungeeComponentSerializer.inject(gson);
        } catch (final Throwable error) {
            Triton.get().getLogger().logError(error, "Failed to inject ProxyServer gson");
        }
    }

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
     * Convert {@link Component} to a {@link BaseComponent} array.
     *
     * @param component The {@link Component} to convert.
     * @return The equivalent of the given {@link Component}, but as a {@link BaseComponent} array.
     * @since 4.0.0
     */
    public static @NotNull BaseComponent @NotNull [] serialize(@NotNull Component component) {
        return BungeeComponentSerializer.get().serialize(component);
    }

    /**
     * Convert {@link Component} to a {@link BaseComponent}.
     *
     * @param component The {@link Component} to convert.
     * @return The equivalent of the given {@link Component}, but as a {@link BaseComponent}.
     * @since 4.0.0
     */
    public static @NotNull BaseComponent serializeToSingle(@NotNull Component component) {
        return convertArrayToSingle(BungeeComponentSerializer.get().serialize(component));
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
        if (components == null) {
            return null;
        }
        if (components.length == 1) {
            return components[0];
        }
        return new TextComponent(components);
    }

}
