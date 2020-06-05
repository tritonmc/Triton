package com.rexcantor64.triton.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.tananaev.jsonpatch.JsonPatch;
import com.tananaev.jsonpatch.JsonPath;
import com.tananaev.jsonpatch.gson.AbsOperationDeserializer;
import com.tananaev.jsonpatch.gson.JsonPathDeserializer;
import com.tananaev.jsonpatch.operation.AbsOperation;
import lombok.val;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TwinParser {

    private static final Gson patchGson = new GsonBuilder()
            .registerTypeAdapter(JsonPath.class, new JsonPathDeserializer())
            .registerTypeAdapter(AbsOperation.class, new AbsOperationDeserializer())
            .create();

    public static ConcurrentHashMap<String, Collection> parseDownload(ConcurrentHashMap<String, Collection> collections, JsonObject data) {
        val deleted = data.getAsJsonArray("deleted");
        val added = data.getAsJsonArray("added");
        val modified = data.getAsJsonObject("modified");
        val metadata = data.getAsJsonObject("metadata");

        // Delete
        collections.values().forEach(collection -> collection.setItems(collection.getItems().stream()
                .filter(item -> !deleted.contains(new JsonPrimitive(item.getTwinData().getId().toString())))
                .collect(Collectors.toList())));

        // Add
        added.forEach(itemElement -> {
            val item = itemElement.getAsJsonObject();
            val collection = collections
                    .computeIfAbsent(item.get("fileName").getAsString(), (ignore) -> new Collection());

            item.remove("fileName");
            collection.getItems().add(TwinManager.gson.fromJson(item, LanguageItem.class));
        });


        // Modify

        // Make a list of translations that need to move to another collection
        val movingCollections = new ConcurrentHashMap<LanguageItem, String>();

        collections.forEach((colName, collection) -> collection.setItems(collection.getItems().stream()
                .map(item -> {
                    val changes = modified.getAsJsonArray(item.getTwinData().getId().toString());
                    if (changes == null) return item; // No changes -> keep the item

                    val itemJson = TwinManager.gson.toJsonTree(item).getAsJsonObject();
                    itemJson.addProperty("fileName", colName);

                    val patch = patchGson.fromJson(changes, JsonPatch.class);

                    val resultJson = patch.apply(itemJson);
                    val result = TwinManager.gson.fromJson(resultJson, LanguageItem.class);

                    val newColName = resultJson.getAsJsonObject().get("fileName").getAsString();
                    if (newColName.equals(colName)) return result;

                    movingCollections.put(result, newColName);
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList())));

        movingCollections
                .forEach((item, col) -> collections.computeIfAbsent(col, k -> new Collection()).getItems().add(item));

        for (val entry : collections.entrySet()) {
            val colMetadata = metadata.getAsJsonObject(entry.getKey());
            if (colMetadata == null) {
                if (entry.getValue().getItems().size() == 0) collections.remove(entry.getKey());
                continue;
            }

            entry.getValue().setMetadata(TwinManager.gson.fromJson(colMetadata, Collection.CollectionMetadata.class));
        }

        return collections;
    }

}
