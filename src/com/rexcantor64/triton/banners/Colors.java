package com.rexcantor64.triton.banners;

public enum Colors {
    BLACK("BLACK", 'a'), RED("RED", 'b'), GREEN("GREEN", 'c'), BROWN("BROWN", 'd'), BLUE(
            "BLUE", 'e'), PURPLE("PURPLE", 'f'), CYAN("CYAN", 'g'), GRAY("SILVER",
            'h'), DARK_GRAY("GRAY", 'i'), PINK("PINK", 'j'), LIME("LIME", 'k'), YELLOW(
            "YELLOW", 'l'), LIGHT_BLUE("LIGHT_BLUE", 'm'), MAGENTA("MAGENTA",
            'n'), ORANGE("ORANGE", 'o'), WHITE("WHITE", 'p');

    Colors(String color, char code) {
        this.color = color;
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public String getColor() {
        return color;
    }

    private final String color;
    private final char code;

    public static Colors getByCode(char code) {
        for (Colors c : values())
            if (c.getCode() == code)
                return c;
        return null;
    }

}
