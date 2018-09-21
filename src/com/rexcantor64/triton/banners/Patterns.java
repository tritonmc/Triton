package com.rexcantor64.triton.banners;

public enum Patterns {
    BASE("BASE", 'a'), BL("SQUARE_BOTTOM_LEFT", 'b'), BO("BORDER", 'c'), BR(
            "SQUARE_BOTTOM_RIGHT",
            'd'), BRI("BRICKS", 'e'), BS("STRIPE_BOTTOM", 'f'), BT("TRIANGLE_BOTTOM",
            'g'), BTS("TRIANGLES_BOTTOM", 'h'), CBO("CURLY_BORDER", 'i'), CR(
            "CROSS",
            'j'), CRE("CREEPER", 'k'), CS("STRIPE_CENTER", 'l'), DLS(
            "STRIPE_DOWNLEFT",
            'm'), DRS("STRIPE_DOWNRIGHT", 'n'), FLO("FLOWER", 'o'), GRA(
            "GRADIENT",
            'p'), HH("HALF_HORIZONTAL", 'q'), LD("DIAGONAL_LEFT",
            'r'), LS("STRIPE_LEFT", 's'), MC(
            "CIRCLE_MIDDLE",
            't'), MOJ("MOJANG", 'u'), MR(
            "RHOMBUS_MIDDLE",
            'v'), MS("STRIPE_MIDDLE", 'w'), RD(
            "DIAGONAL_RIGHT",
            'x'), RS("STRIPE_RIGHT", 'y'), SC(
            "STRAIGHT_CROSS",
            'z'), SKU("SKULL",
            'A'), SS(
            "STRIPE_SMALL",
            'B'), TL(
            "SQUARE_TOP_LEFT",
            'C'), TR(
            "SQUARE_TOP_RIGHT",
            'D'), TS(
            "STRIPE_TOP",
            'E'), TT(
            "TRIANGLE_TOP",
            'F'), TTS(
            "TRIANGLES_TOP",
            'G'), VH(
            "HALF_VERTICAL",
            'H'), LUD(
            "DIAGONAL_LEFT_MIRROR",
            'I'), RUD(
            "DIAGONAL_RIGHT_MIRROR",
            'J'), GRU(
            "GRADIENT_UP",
            'K'), HHB(
            "HALF_HORIZONTAL_MIRROR",
            'L'), VHR(
            "HALF_VERTICAL_MIRROR",
            'M');

    Patterns(String type, char code) {
        this.type = type;
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public String getType() {
        return type;
    }

    private final String type;
    private final char code;

    public static Patterns getByCode(char code) {
        for (Patterns p : values())
            if (p.getCode() == code)
                return p;
        return null;
    }

}
