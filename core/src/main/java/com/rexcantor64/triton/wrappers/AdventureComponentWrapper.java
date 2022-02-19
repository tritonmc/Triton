package com.rexcantor64.triton.wrappers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class AdventureComponentWrapper {

    public static BaseComponent[] toMd5Component(Object component) {
        return ComponentSerializer.parse(toJson(component));
    }

    public static String toJson(Object component) {
        return GsonComponentSerializer.gson().serialize((Component) component);
    }

}
