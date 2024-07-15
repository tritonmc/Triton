package com.rexcantor64.triton.spigot.banners;

import lombok.Getter;
import org.bukkit.block.banner.PatternType;

import java.util.Arrays;

@Getter
public enum Patterns {
    BASE('a', "BASE"),
    BL('b', "SQUARE_BOTTOM_LEFT"),
    BO('c', "BORDER"),
    BR('d', "SQUARE_BOTTOM_RIGHT"),
    BRI('e', "BRICKS"),
    BS('f', "STRIPE_BOTTOM"),
    BT('g', "TRIANGLE_BOTTOM"),
    BTS('h', "TRIANGLES_BOTTOM"),
    CBO('i', "CURLY_BORDER"),
    CR('j', "CROSS"),
    CRE('k', "CREEPER"),
    CS('l', "STRIPE_CENTER"),
    DLS('m', "STRIPE_DOWNLEFT"),
    DRS('n', "STRIPE_DOWNRIGHT"),
    FLO('o', "FLOWER"),
    GRA('p', "GRADIENT"),
    HH('q', "HALF_HORIZONTAL"),
    LD('r', "DIAGONAL_LEFT"),
    LS('s', "STRIPE_LEFT"),
    MC('t', "CIRCLE", "CIRCLE_MIDDLE"),
    MOJ('u', "MOJANG"),
    MR('v', "RHOMBUS", "RHOMBUS_MIDDLE"),
    MS('w', "STRIPE_MIDDLE"),
    RD('x', "DIAGONAL_RIGHT"),
    RS('y', "STRIPE_RIGHT"),
    SC('z', "STRAIGHT_CROSS"),
    SKU('A', "SKULL"),
    SS('B', "SMALL_STRIPES", "STRIPE_SMALL"),
    TL('C', "SQUARE_TOP_LEFT"),
    TR('D', "SQUARE_TOP_RIGHT"),
    TS('E', "STRIPE_TOP"),
    TT('F', "TRIANGLE_TOP"),
    TTS('G', "TRIANGLES_TOP"),
    VH('H', "HALF_VERTICAL"),
    LUD('I', "DIAGONAL_UP_LEFT", "DIAGONAL_LEFT_MIRROR"),
    RUD('J', "DIAGONAL_UP_RIGHT", "DIAGONAL_RIGHT_MIRROR"),
    GRU('K', "GRADIENT_UP"),
    HHB('L', "HALF_HORIZONTAL_BOTTOM", "HALF_HORIZONTAL_MIRROR"),
    VHR('M', "HALF_VERTICAL_RIGHT", "HALF_VERTICAL_MIRROR");

    private final char code;
    private final String[] typeAliases;

    Patterns(char code, String... typeAliases) {
        this.code = code;
        this.typeAliases = typeAliases;
    }

    public static Patterns getByCode(char code) {
        for (Patterns p : values())
            if (p.getCode() == code)
                return p;
        return null;
    }

    public PatternType toPatternType() {
        for (String alias : this.typeAliases) {
            try {
                return PatternType.valueOf(alias);
            } catch (IllegalArgumentException ignore) {
            }
        }
        throw new IllegalArgumentException("Cannot find corresponding pattern type to " + Arrays.toString(this.typeAliases));
    }

}
