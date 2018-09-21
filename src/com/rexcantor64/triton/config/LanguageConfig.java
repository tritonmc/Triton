package com.rexcantor64.triton.config;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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
            File file = new File(MultiLanguagePlugin.get().getDataFolder(), useCache ? "languages.cache.json" : "languages.json");
            if (!file.exists()) {
                try {
                    Files.write(file.toPath(), "[]".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                } catch (Exception e) {
                    MultiLanguagePlugin.get().logDebugWarning("Failed to create %1! Error: %2", file.getAbsolutePath(), e.getMessage());
                }
                return;
            }
            raw = new JSONArray(FileUtils.contentsToString(file));
            for (int i = 0; i < raw.length(); i++) {
                LanguageItem item = LanguageItem.fromJSON(raw.optJSONObject(i));
                if (item == null) continue;
                items.add(item);
            }
        } catch (Exception e) {
            MultiLanguagePlugin.get().logWarning("An error occurred while loading languages! Some language items may not have been loaded! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Loaded");
        }
    }

    private void logCount(long timeStarted, String action) {
        MultiLanguagePlugin.get().logDebug(action + " %1 language items in %2 ms!", items.size(), System.currentTimeMillis() - timeStarted);
    }

    public void saveToCache() {
        long timeStarted = System.currentTimeMillis();
        try {
            JSONArray array = new JSONArray();
            for (LanguageItem item : items) {
                JSONObject obj = new JSONObject();
                if (item.getType() == LanguageItem.LanguageItemType.TEXT) {
                    LanguageText lt = (LanguageText) item;
                    obj.put("type", "text");
                    obj.put("key", lt.getKey());
                    obj.put("languages", lt.getLanguages());
                } else if (item.getType() == LanguageItem.LanguageItemType.SIGN) {
                    LanguageSign lt = (LanguageSign) item;
                    obj.put("type", "sign");
                    JSONObject loc = new JSONObject();
                    loc.put("world", lt.getLocation().getWorld());
                    loc.put("x", lt.getLocation().getX());
                    loc.put("y", lt.getLocation().getY());
                    loc.put("z", lt.getLocation().getZ());
                    obj.put("location", loc);
                    obj.put("lines", lt.getLanguages());
                }
                array.put(obj);
            }
            File file = new File(MultiLanguagePlugin.get().getDataFolder(), "languages.cache.json");
            Files.write(file.toPath(), array.toString(4).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            MultiLanguagePlugin.get().logWarning("An error occurred while saving language items to cache! Some items may not be saved if there is a server shutdown! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Saved");
        }
    }
}
