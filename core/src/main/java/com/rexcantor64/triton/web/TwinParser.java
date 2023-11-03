package com.rexcantor64.triton.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.tananaev.jsonpatch.JsonPatch;
import com.tananaev.jsonpatch.JsonPath;
import com.tananaev.jsonpatch.gson.AbsOperationDeserializer;
import com.tananaev.jsonpatch.gson.JsonPathDeserializer;
import com.tananaev.jsonpatch.operation.AbsOperation;
import lombok.Data;
import lombok.val;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TwinParser {

    private static final Gson patchGson = new GsonBuilder()
            .registerTypeAdapter(JsonPath.class, new JsonPathDeserializer())
            .registerTypeAdapter(AbsOperation.class, new AbsOperationDeserializer())
            .create();

    public static TwinResponse parseDownload(ConcurrentHashMap<String, Collection> collections, JsonObject data) {
        val deleted = jsonArrayToArrayList(data.getAsJsonArray("deleted"));
        val added = data.getAsJsonArray("added");
        val modified = data.getAsJsonObject("modified");
        val metadata = data.getAsJsonObject("metadata");

        val changedList = new ArrayList<LanguageItem>();
        val deletedList = new ArrayList<LanguageItem>();

        // Delete
        collections.values().forEach(collection -> collection.setItems(collection.getItems().stream()
                .filter(item -> {
                    if (item.getTwinData() == null || item.getTwinData().getId() == null) return true;
                    if (!deleted.contains(item.getTwinData().getId().toString())) return true;
                    deletedList.add(item);
                    return false;
                })
                .collect(Collectors.toList())));

        // Add
        added.forEach(itemElement -> {
            val itemJson = itemElement.getAsJsonObject();
            val fileName = itemJson.get("fileName");
            if (fileName == null || !fileName.isJsonPrimitive()) {
                throw new JsonParseException("Translation does not belong to any collection: " + itemJson);
            }
            val collection = collections
                    .computeIfAbsent(fileName.getAsString(), (ignore) -> new Collection());

            itemJson.remove("fileName");

            val item = TwinManager.gson.fromJson(itemJson, LanguageItem.class);
            collection.getItems().add(item);

            changedList.add(item);
        });


        // Modify

        // Make a list of translations that need to move to another collection
        val movingCollections = new ConcurrentHashMap<LanguageItem, String>();

        collections.forEach((colName, collection) -> collection.setItems(collection.getItems().stream()
                .map(item -> {
                    if (item.getTwinData() == null || item.getTwinData().getId() == null) return item;
                    val changes = modified.getAsJsonArray(item.getTwinData().getId().toString());
                    if (changes == null) return item; // No changes -> keep the item

                    val itemJson = TwinManager.gson.toJsonTree(item).getAsJsonObject();
                    itemJson.addProperty("fileName", colName);

                    val patch = patchGson.fromJson(changes, JsonPatch.class);

                    val resultJson = patch.apply(itemJson);
                    val result = TwinManager.gson.fromJson(resultJson, LanguageItem.class);

                    changedList.add(result);

                    val newColNameElement = resultJson.getAsJsonObject().get("fileName");
                    if (newColNameElement == null || !newColNameElement.isJsonPrimitive()) {
                        Triton.get().getLogger().logWarning("Could not calculate the target collection to move the translation into: " + resultJson);
                        return result;
                    } else {
                        val newColName = resultJson.getAsJsonObject().get("fileName").getAsString();
                        if (newColName.equals(colName)) return result;

                        movingCollections.put(result, newColName);

                        return null;
                    }
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

        return new TwinResponse(collections, changedList, deletedList);
    }

    private static List<String> jsonArrayToArrayList(JsonArray array) {
        val list = new ArrayList<String>();
        array.iterator().forEachRemaining((element) -> list.add(element.getAsString()));
        return list;
    }

    @Data
    public static class TwinResponse {
        private final ConcurrentHashMap<String, Collection> collections;
        private final List<LanguageItem> changed;
        private final List<LanguageItem> deleted;
    }

}
