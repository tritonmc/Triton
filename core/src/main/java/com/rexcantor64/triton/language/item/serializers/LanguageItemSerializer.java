package com.rexcantor64.triton.language.item.serializers;

import com.google.gson.*;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.TWINData;
import lombok.val;

import java.lang.reflect.Type;

public class LanguageItemSerializer implements JsonDeserializer<LanguageItem> {

    static void deserialize(JsonObject json, LanguageItem item, JsonDeserializationContext context) throws JsonParseException {
        val key = json.get("key");
        if (key == null || !key.isJsonPrimitive()) throw new JsonParseException("Translation requires a key");
        item.setKey(key.getAsString());

        val twinData = json.get("_twin");
        if (twinData != null && twinData.isJsonObject())
            item.setTwinData(context.deserialize(twinData, TWINData.class));
    }

    static void serialize(LanguageItem item, JsonObject json, JsonSerializationContext context) {
        json.addProperty("key", item.getKey());
        json.addProperty("type", item.getType().getName());

        if (item.getTwinData() != null)
            json.add("_twin", context.serialize(item.getTwinData(), TWINData.class));
    }

    @Override
    public LanguageItem deserialize(JsonElement json, Type t, JsonDeserializationContext context) throws JsonParseException {
        val obj = json.getAsJsonObject();
        val typeElement = obj.get("type");
        if (typeElement == null || !typeElement.isJsonPrimitive()) throw new JsonParseException("Translation type is not present: " + json);
        val type = typeElement.getAsString();

        if (type.equalsIgnoreCase("text"))
            return context.deserialize(json, LanguageText.class);
        if (type.equalsIgnoreCase("sign"))
            return context.deserialize(json, LanguageSign.class);

        throw new JsonParseException("Invalid translation type: " + type);
    }

}
