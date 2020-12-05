package com.rexcantor64.triton.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;

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
        target.setInsertion(origin.getInsertion());
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
        return c1.getColorRaw() == c2.getColorRaw() &&
                c1.isBoldRaw() == c2.isBoldRaw() &&
                c1.isItalicRaw() == c2.isItalicRaw() &&
                c1.isUnderlinedRaw() == c2.isUnderlinedRaw() &&
                c1.isStrikethroughRaw() == c2.isStrikethroughRaw() &&
                c1.isObfuscatedRaw() == c2.isObfuscatedRaw() &&
                (c1.getInsertion() == null ? c2.getInsertion() == null : c1.getInsertion().equals(c2.getInsertion())) &&
                (c1.getHoverEvent() == null ? c2.getHoverEvent() == null : c1.getHoverEvent()
                        .equals(c2.getHoverEvent())) &&
                (c1.getClickEvent() == null ? c2.getClickEvent() == null : c1.getClickEvent()
                        .equals(c2.getClickEvent()));
    }

}
