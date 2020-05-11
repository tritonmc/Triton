package com.rexcantor64.triton.language.item.serializers;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.language.item.LanguageText;
import lombok.val;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class LanguageTextSerializer implements JsonSerializer<LanguageText>, JsonDeserializer<LanguageText> {
    private static final Type TEXT_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    @Override
    public LanguageText deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        val item = new LanguageText();
        val obj = json.getAsJsonObject();

        LanguageItemSerializer.deserialize(obj, item, context);

        if (obj.has("text"))
            item.setLanguages(context.deserialize(obj.get("text"), TEXT_TYPE));
        if (obj.has("patterns"))
            item.setPatterns(context.deserialize(obj.get("patterns"), STRING_LIST_TYPE));

        if (obj.has("blacklist"))
            item.setBlacklist(obj.get("blacklist").getAsBoolean());
        if (obj.has("servers"))
            item.setServers(context.deserialize(obj.get("servers"), STRING_LIST_TYPE));

        item.generateRegexStrings();
        return item;
    }

    @Override
    public JsonElement serialize(LanguageText item, Type type, JsonSerializationContext context) {
        val json = new JsonObject();

        LanguageItemSerializer.serialize(item, json, context);

        json.addProperty("type", "text");

        if (item.getLanguages() != null)
            json.add("text", context.serialize(item.getLanguages(), TEXT_TYPE));
        if (item.getPatterns() != null && item.getPatterns().size() > 0)
            json.add("patterns", context.serialize(item.getPatterns(), STRING_LIST_TYPE));

        if (item.getBlacklist() != null)
            json.addProperty("patterns", item.getBlacklist());
        if (item.getServers() != null && (item.getServers().size() > 0 || item.getBlacklist() == Boolean.FALSE))
            json.add("servers", context.serialize(item.getServers(), STRING_LIST_TYPE));

        return null;
    }
}
