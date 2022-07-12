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
import java.util.Objects;
import java.util.stream.Collectors;

public class CollectionSerializer implements JsonDeserializer<Collection> {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Collection.class, new CollectionSerializer())
            .registerTypeAdapter(LanguageItem.class, new LanguageItemSerializer())
            .registerTypeAdapter(LanguageText.class, new LanguageTextSerializer())
            .registerTypeAdapter(LanguageSign.class, new LanguageSignSerializer())
            .create();

    public static Collection parse(Reader json) {
        return gson.fromJson(json, Collection.class);
    }

    public static void toJson(Collection collection, Appendable reader) {
        gson.toJson(collection, reader);
    }

    @Override
    public Collection deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        val collection = new Collection();

        JsonArray items;
        if (json.isJsonArray()) {
            items = json.getAsJsonArray();
        } else if (json.isJsonObject()) {
            items = json.getAsJsonObject().getAsJsonArray("items");
            final Collection.CollectionMetadata metadata = context.deserialize(
                    json.getAsJsonObject().get("metadata"),
                    Collection.CollectionMetadata.class
            );
            if (metadata != null) {
                collection.setMetadata(metadata);
            }
        } else {
            throw new JsonParseException("Invalid JSON while deserializing Collection");
        }

        collection.setItems(Arrays.stream((LanguageItem[]) context.deserialize(items, LanguageItem[].class))
                .filter(Objects::nonNull).collect(Collectors.toList()));

        return collection;
    }

}
