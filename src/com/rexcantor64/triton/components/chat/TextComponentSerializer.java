package com.rexcantor64.triton.components.chat;

import com.google.gson.*;
import com.rexcantor64.triton.components.api.chat.BaseComponent;
import com.rexcantor64.triton.components.api.chat.TextComponent;

import java.lang.reflect.Type;
import java.util.List;

public class TextComponentSerializer
        extends BaseComponentSerializer
        implements JsonSerializer<TextComponent>, JsonDeserializer<TextComponent> {
    public TextComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        TextComponent component = new TextComponent();
        JsonObject object = json.getAsJsonObject();
        deserialize(object, component, context);
        component.setText(object.get("text").getAsString());
        return component;
    }

    public JsonElement serialize(TextComponent src, Type typeOfSrc, JsonSerializationContext context) {
        List<BaseComponent> extra = src.getExtra();
        JsonObject object = new JsonObject();
        if ((src.hasFormatting()) || ((extra != null) && (!extra.isEmpty()))) {
            serialize(object, src, context);
        }
        object.addProperty("text", src.getText());
        return object;
    }
}