package com.rexcantor64.triton.wrappers.legacy;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;

public class HoverComponentWrapper {

    public static BaseComponent[] getValue(HoverEvent hover) {
        return hover.getValue();
    }

    public static HoverEvent setValue(HoverEvent hover, BaseComponent... components) {
        return new HoverEvent(hover.getAction(), components);
    }

}
