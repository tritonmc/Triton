package com.rexcantor64.triton.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;

import java.util.Objects;
import java.util.regex.Pattern;

public class ComponentUtils {

    private static final Pattern url = Pattern.compile("^((([A-Za-z]{3,9}:(?:\\/\\/)?)(?:[-;:&=\\+\\$,\\w]+@)" +
            "?[A-Za-z0-9.-]+(:[0-9]+)?|(?:www.|[-;:&=\\+\\$,\\w]+@)[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??" +
            "(?:[-\\+=&;%@.\\w_/]*)#?(?:[\\w]*))?)$");

    public static int encodeClickAction(ClickEvent.Action action) {
        switch (action) {
            case OPEN_URL:
                return 0;
            case OPEN_FILE:
                return 1;
            case RUN_COMMAND:
                return 2;
            case SUGGEST_COMMAND:
                return 3;
            case CHANGE_PAGE:
                return 4;
            case COPY_TO_CLIPBOARD:
                return 5;
        }
        return 0;
    }

    public static ClickEvent.Action decodeClickAction(int action) {
        switch (action) {
            default:
                return ClickEvent.Action.OPEN_URL;
            case 1:
                return ClickEvent.Action.OPEN_FILE;
            case 2:
                return ClickEvent.Action.RUN_COMMAND;
            case 3:
                return ClickEvent.Action.SUGGEST_COMMAND;
            case 4:
                return ClickEvent.Action.CHANGE_PAGE;
            case 5:
                return ClickEvent.Action.COPY_TO_CLIPBOARD;
        }
    }

    public static boolean isLink(String text) {
        return url.matcher(text).find();
    }

    public static void copyFormatting(BaseComponent origin, BaseComponent target) {
        target.setColor(origin.getColorRaw());
        target.setBold(origin.isBoldRaw());
        target.setItalic(origin.isItalicRaw());
        target.setUnderlined(origin.isUnderlinedRaw());
        target.setStrikethrough(origin.isStrikethroughRaw());
        target.setObfuscated(origin.isObfuscatedRaw());
        try {
            target.setInsertion(origin.getInsertion());
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
    }

    public static ChatColor getColorFromBaseComponent(BaseComponent bc) {
        if (bc.getColorRaw() != null)
            return bc.getColorRaw();
        Object parent = NMSUtils.getDeclaredField(bc, "parent");
        return !(parent instanceof BaseComponent) ? ChatColor.RESET :
                getColorFromBaseComponent((BaseComponent) parent);
    }

    public static boolean hasExtra(BaseComponent bc) {
        return bc.getExtra() != null && bc.getExtra().size() != 0;
    }

    public static boolean haveSameFormatting(BaseComponent c1, BaseComponent c2) {
        String insertion1 = null;
        String insertion2 = null;
        try {
            insertion1 = c1.getInsertion();
            insertion2 = c2.getInsertion();
        } catch (NoSuchMethodError ignore) {
            // Ignore, it's an outdated server
        }
        return c1.getColorRaw() == c2.getColorRaw() &&
                c1.isBoldRaw() == c2.isBoldRaw() &&
                c1.isItalicRaw() == c2.isItalicRaw() &&
                c1.isUnderlinedRaw() == c2.isUnderlinedRaw() &&
                c1.isStrikethroughRaw() == c2.isStrikethroughRaw() &&
                c1.isObfuscatedRaw() == c2.isObfuscatedRaw() &&
                Objects.equals(insertion1, insertion2) &&
                Objects.equals(c1.getHoverEvent(), c2.getHoverEvent()) &&
                Objects.equals(c1.getClickEvent(), c2.getClickEvent());
    }

}
