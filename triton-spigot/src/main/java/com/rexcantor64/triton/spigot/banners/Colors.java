package com.rexcantor64.triton.spigot.banners;

import lombok.Getter;
import org.bukkit.DyeColor;

import java.util.Arrays;

@Getter
public enum Colors {
    BLACK('a', "BLACK"),
    RED('b', "RED"),
    GREEN('c', "GREEN"),
    BROWN('d', "BROWN"),
    BLUE('e', "BLUE"),
    PURPLE('f', "PURPLE"),
    CYAN('g', "CYAN"),
    GRAY('h', "LIGHT_GRAY", "SILVER"),
    DARK_GRAY('i', "GRAY"),
    PINK('j', "PINK"),
    LIME('k', "LIME"),
    YELLOW('l', "YELLOW"),
    LIGHT_BLUE('m', "LIGHT_BLUE"),
    MAGENTA('n', "MAGENTA"),
    ORANGE('o', "ORANGE"),
    WHITE('p', "WHITE");

    private final char code;
    private final String[] colorAliases; // some colors have been renamed throughout Spigot versions

    Colors(char code, String... colorAliases) {
        this.code = code;
        this.colorAliases = colorAliases;
    }

    public static Colors getByCode(char code) {
        for (Colors c : values())
            if (c.getCode() == code)
                return c;
        return null;
    }

    public DyeColor toDyeColor() {
        for (String alias : this.colorAliases) {
            try {
                return DyeColor.valueOf(alias);
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new IllegalArgumentException("Cannot find corresponding dye color to " + Arrays.toString(this.colorAliases));
    }

}
