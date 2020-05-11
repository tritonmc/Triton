package com.rexcantor64.triton.language.item.serializers;

import com.google.gson.*;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import lombok.val;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Arrays;

public class CollectionSerializer implements JsonDeserializer<Collection> {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Collection.class, new CollectionSerializer())
            .registerTypeAdapter(LanguageItem.class, new LanguageItemSerializer())
            .registerTypeAdapter(LanguageText.class, new LanguageTextSerializer())
            .registerTypeAdapter(LanguageSign.class, new LanguageSignSerializer())
            .create();

    public static Collection parse(Reader json) {
        return gson.fromJson(json, Collection.class);
    }

    public static String toJson(Collection collection) {
        return gson.toJson(collection);
    }

    @Override
    public Collection deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        val collection = new Collection();

        JsonArray items;
        if (json.isJsonArray()) {
            items = json.getAsJsonArray();
        } else if (json.isJsonObject()) {
            items = json.getAsJsonObject().getAsJsonArray("items");
            collection.setMetadata(context
                    .deserialize(json.getAsJsonObject().get("metadata"), Collection.CollectionMetadata.class));
        } else {
            throw new JsonParseException("Invalid JSON while deserializing Collection");
        }

        collection.setItems(Arrays.asList(context.deserialize(items, LanguageItem[].class)));

        return collection;
    }

}
