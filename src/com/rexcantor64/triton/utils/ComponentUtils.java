package com.rexcantor64.triton.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class ComponentUtils {

    private static final Pattern url = Pattern.compile("^(?:(https?)://)?([-\\w_.]{2,}\\.[a-z]{2,4})(/\\S*)?$");

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

    public static int encodeHoverAction(HoverEvent.Action action) {
        switch (action) {
            case SHOW_TEXT:
                return 0;
            case SHOW_ACHIEVEMENT:
                return 1;
            case SHOW_ITEM:
                return 2;
            case SHOW_ENTITY:
                return 3;
        }
        return 0;
    }

    public static HoverEvent.Action decodeHoverAction(int action) {
        switch (action) {
            default:
                return HoverEvent.Action.SHOW_TEXT;
            case 1:
                return HoverEvent.Action.SHOW_ACHIEVEMENT;
            case 2:
                return HoverEvent.Action.SHOW_ITEM;
            case 3:
                return HoverEvent.Action.SHOW_ENTITY;
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

    public static BaseComponent[] removeEmptyExtras(BaseComponent... comps) {
        for (BaseComponent comp : comps) {
            if (comp.getExtra() == null)
                continue;
            if (comp.getExtra().isEmpty()) {
                try {
                    Field f = BaseComponent.class.getDeclaredField("extra");
                    f.setAccessible(true);
                    f.set(comp, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }
            removeEmptyExtras(comp.getExtra().toArray(new BaseComponent[0]));
        }
        return comps;
    }

    public static ChatColor getColorFromBaseComponent(BaseComponent bc) {
        if (bc.getColorRaw() != null)
            return bc.getColorRaw();
        Object parent = NMSUtils.getDeclaredField(bc, "parent");
        return !(parent instanceof BaseComponent) ? ChatColor.RESET :
                getColorFromBaseComponent((BaseComponent) parent);
    }

}
