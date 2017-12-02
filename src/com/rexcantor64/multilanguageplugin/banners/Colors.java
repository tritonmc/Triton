package com.rexcantor64.multilanguageplugin.banners;

import org.bukkit.DyeColor;

public enum Colors {
    BLACK(DyeColor.BLACK, 'a'), RED(DyeColor.RED, 'b'), GREEN(DyeColor.GREEN, 'c'), BROWN(DyeColor.BROWN, 'd'), BLUE(
            DyeColor.BLUE, 'e'), PURPLE(DyeColor.PURPLE, 'f'), CYAN(DyeColor.CYAN, 'g'), GRAY(DyeColor.SILVER,
            'h'), DARK_GRAY(DyeColor.GRAY, 'i'), PINK(DyeColor.PINK, 'j'), LIME(DyeColor.LIME, 'k'), YELLOW(
            DyeColor.YELLOW, 'l'), LIGHT_BLUE(DyeColor.LIGHT_BLUE, 'm'), MAGENTA(DyeColor.MAGENTA,
            'n'), ORANGE(DyeColor.ORANGE, 'o'), WHITE(DyeColor.WHITE, 'p');

    Colors(DyeColor color, char code) {
        this.color = color;
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public DyeColor getColor() {
        return color;
    }

    private final DyeColor color;
    private final char code;

    public static Colors getByCode(char code) {
        for (Colors c : values())
            if (c.getCode() == code)
                return c;
        return null;
    }

}
