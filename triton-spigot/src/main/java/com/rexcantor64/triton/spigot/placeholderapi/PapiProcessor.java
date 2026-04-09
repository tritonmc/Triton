package com.rexcantor64.triton.spigot.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PapiProcessor {

    /**
     * @see PlaceholderAPI#setPlaceholders(Player, String)
     * @since 4.0.0
     */
    public static @NotNull String replacePlaceholders(@NotNull String message, @NotNull Player player) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }
}
