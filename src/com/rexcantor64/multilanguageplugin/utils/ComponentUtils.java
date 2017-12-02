package com.rexcantor64.multilanguageplugin.utils;

import com.rexcantor64.multilanguageplugin.components.api.chat.BaseComponent;

public class ComponentUtils {

    public static BaseComponent copyFormatting(BaseComponent source, BaseComponent target) {
        target.setColor(source.getTrueColor());
        target.setBold(source.isTrueBold());
        target.setItalic(source.isTrueItalic());
        target.setUnderlined(source.isTrueUnderlined());
        target.setStrikethrough(source.isTrueStrikethrough());
        target.setObfuscated(source.isTrueObfuscated());
        target.setClickEvent(source.getClickEvent());
        target.setHoverEvent(source.getHoverEvent());
        return target;
    }



}
