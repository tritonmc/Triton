package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class LanguageConfig {

    private JSONArray raw;
    private List<LanguageItem> items = new ArrayList<>();

    public List<LanguageItem> getItems() {
        return items;
    }

    public LanguageConfig setItems(List<LanguageItem> items) {
        this.items = items;
        return this;
    }

    public JSONArray getRaw() {
        return raw;
    }

    public void setup(boolean useCache) {
        items.clear();
        long timeStarted = System.currentTimeMillis();
        try {
            File file = new File(Triton.get().getDataFolder(), useCache ? "languages.cache.json" : "languages.json");
            if (!file.exists()) {
                try {
                    if (!useCache)
                        Files.write(file.toPath(), "[]".getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (Exception e) {
                    Triton.get().logDebugWarning("Failed to create %1! Error: %2", file.getAbsolutePath(), e.getMessage());
                }
                return;
            }
            setup(new JSONArray(FileUtils.contentsToString(file)));
        } catch (Exception e) {
            Triton.get().logWarning("An error occurred while loading languages! Some language items may not have been loaded! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Loaded");
        }
    }

    private void setup(JSONArray raw) {
        this.raw = raw;
        for (int i = 0; i < raw.length(); i++) {
            LanguageItem item = LanguageItem.fromJSON(raw.optJSONObject(i));
            if (item == null) continue;
            items.add(item);
        }
    }

    public void saveFromRaw(JSONArray raw) {
        long timeStarted = System.currentTimeMillis();
        try {
            File file = new File(Triton.get().getDataFolder(), "languages.json");
            Files.write(file.toPath(), raw.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            items.clear();
        } catch (Exception e) {
            Triton.get().logWarning("An error occurred while saving language items after sign update! Some items may not be saved if there is a server shutdown! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Saved");
        }
    }

    private void logCount(long timeStarted, String action) {
        Triton.get().logDebug(action + " %1 language items in %2 ms!", items.size(), System.currentTimeMillis() - timeStarted);
    }

    public void saveToCache() {
        long timeStarted = System.currentTimeMillis();
        try {
            JSONArray array = new JSONArray();
            for (LanguageItem item : items) {
                JSONObject obj = new JSONObject();
                obj.put("key", item.getKey());
                if (item.getType() == LanguageItem.LanguageItemType.TEXT) {
                    LanguageText lt = (LanguageText) item;
                    obj.put("type", "text");
                    obj.put("languages", lt.getLanguages());
                } else if (item.getType() == LanguageItem.LanguageItemType.SIGN) {
                    LanguageSign ls = (LanguageSign) item;
                    obj.put("type", "sign");
                    JSONArray locs = new JSONArray();
                    for (LanguageSign.SignLocation location : ls.getLocations()) {
                        JSONObject loc = new JSONObject();
                        loc.put("world", location.getWorld());
                        loc.put("x", location.getX());
                        loc.put("y", location.getY());
                        loc.put("z", location.getZ());
                        locs.put(loc);
                    }
                    obj.put("locations", locs);
                    obj.put("lines", ls.getLanguages());
                }
                array.put(obj);
            }
            File file = new File(Triton.get().getDataFolder(), "languages.cache.json");
            Files.write(file.toPath(), array.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            Triton.get().logWarning("An error occurred while saving language items to cache! Some items may not be saved if there is a server shutdown! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Saved");
        }
    }
}
