package com.rexcantor64.triton.wrappers;

import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class AdventureComponentWrapper {

    public static BaseComponent[] toMd5Component(Object comp) {
        val json = GsonComponentSerializer.gson().serialize((Component) comp);
        return ComponentSerializer.parse(json);
    }

}
