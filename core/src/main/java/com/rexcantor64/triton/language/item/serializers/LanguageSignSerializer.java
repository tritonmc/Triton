package com.rexcantor64.triton.language.item.serializers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.SignLocation;
import lombok.val;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

public class LanguageSignSerializer implements JsonSerializer<LanguageSign>, JsonDeserializer<LanguageSign> {
    private static final Type LANGUAGES_TYPE = new TypeToken<HashMap<String, String[]>>() {
    }.getType();
    private static final Type LOCATIONS_TYPE = new TypeToken<List<SignLocation>>() {
    }.getType();

    @Override
    public LanguageSign deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        val item = new LanguageSign();
        val obj = json.getAsJsonObject();

        LanguageItemSerializer.deserialize(obj, item, context);

        if (obj.has("lines"))
            item.setLines(context.deserialize(obj.get("lines"), LANGUAGES_TYPE));

        if (obj.has("locations"))
            item.setLocations(context.deserialize(obj.get("locations"), LOCATIONS_TYPE));

        return item;
    }

    @Override
    public JsonElement serialize(LanguageSign item, Type type, JsonSerializationContext context) {
        val json = new JsonObject();

        LanguageItemSerializer.serialize(item, json, context);

        if (item.getLines() != null)
            json.add("lines", context.serialize(item.getLines(), LANGUAGES_TYPE));

        if (item.getLocations() != null)
            json.add("locations", context.serialize(item.getLocations(), LOCATIONS_TYPE));

        return json;
    }
}
