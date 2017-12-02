package com.rexcantor64.multilanguageplugin.banners;

import org.bukkit.block.banner.PatternType;

public enum Patterns {
    BASE(PatternType.BASE, 'a'), BL(PatternType.SQUARE_BOTTOM_LEFT, 'b'), BO(PatternType.BORDER, 'c'), BR(
            PatternType.SQUARE_BOTTOM_RIGHT,
            'd'), BRI(PatternType.BRICKS, 'e'), BS(PatternType.STRIPE_BOTTOM, 'f'), BT(PatternType.TRIANGLE_BOTTOM,
            'g'), BTS(PatternType.TRIANGLES_BOTTOM, 'h'), CBO(PatternType.CURLY_BORDER, 'i'), CR(
            PatternType.CROSS,
            'j'), CRE(PatternType.CREEPER, 'k'), CS(PatternType.STRIPE_CENTER, 'l'), DLS(
            PatternType.STRIPE_DOWNLEFT,
            'm'), DRS(PatternType.STRIPE_DOWNRIGHT, 'n'), FLO(PatternType.FLOWER, 'o'), GRA(
            PatternType.GRADIENT,
            'p'), HH(PatternType.HALF_HORIZONTAL, 'q'), LD(PatternType.DIAGONAL_LEFT,
            'r'), LS(PatternType.STRIPE_LEFT, 's'), MC(
            PatternType.CIRCLE_MIDDLE,
            't'), MOJ(PatternType.MOJANG, 'u'), MR(
            PatternType.RHOMBUS_MIDDLE,
            'v'), MS(PatternType.STRIPE_MIDDLE, 'w'), RD(
            PatternType.DIAGONAL_RIGHT,
            'x'), RS(PatternType.STRIPE_RIGHT, 'y'), SC(
            PatternType.STRAIGHT_CROSS,
            'z'), SKU(PatternType.SKULL,
            'A'), SS(
            PatternType.STRIPE_SMALL,
            'B'), TL(
            PatternType.SQUARE_TOP_LEFT,
            'C'), TR(
            PatternType.SQUARE_TOP_RIGHT,
            'D'), TS(
            PatternType.STRIPE_TOP,
            'E'), TT(
            PatternType.TRIANGLE_TOP,
            'F'), TTS(
            PatternType.TRIANGLES_TOP,
            'G'), VH(
            PatternType.HALF_VERTICAL,
            'H'), LUD(
            PatternType.DIAGONAL_LEFT_MIRROR,
            'I'), RUD(
            PatternType.DIAGONAL_RIGHT_MIRROR,
            'J'), GRU(
            PatternType.GRADIENT_UP,
            'K'), HHB(
            PatternType.HALF_HORIZONTAL_MIRROR,
            'L'), VHR(
            PatternType.HALF_VERTICAL_MIRROR,
            'M');

    Patterns(PatternType type, char code) {
        this.type = type;
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public PatternType getType() {
        return type;
    }

    private final PatternType type;
    private final char code;

    public static Patterns getByCode(char code) {
        for (Patterns p : values())
            if (p.getCode() == code)
                return p;
        return null;
    }

}
