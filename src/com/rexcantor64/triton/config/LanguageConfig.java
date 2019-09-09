package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class LanguageConfig {

    private List<LanguageItem> items = new ArrayList<>();

    public List<LanguageItem> getItems() {
        return items;
    }

    public LanguageConfig setItems(List<LanguageItem> items) {
        this.items = items;
        return this;
    }

    public void setup(boolean useCache) {
        items.clear();
        long timeStarted = System.currentTimeMillis();
        try {
            if (useCache) {
                File cacheFile = new File(Triton.get().getDataFolder(), "translations.cache.json");
                if (cacheFile.exists()) {
                    setup(null, new JSONArray(FileUtils.contentsToString(cacheFile)), "cache");
                }
            } else {
                File translationFolder = Triton.get().getTranslationsFolder();
                if (!translationFolder.isDirectory()) {
                    if (translationFolder.mkdirs()) {
                        File defaultFile = new File(translationFolder, "default.json");
                        Files.write(defaultFile.toPath(), "[]".getBytes(StandardCharsets.UTF_8),
                                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                    }
                }
                File[] files = translationFolder.listFiles();
                if (files != null) {
                    Triton.get().logDebug("Found %1 translation files.", files.length);
                    for (File f : files)
                        setupFromFile(f);
                }
            }
        } catch (Exception e) {
            Triton.get().logWarning("An error occurred while loading translations! Some translation items may not " +
                    "have been" +
                    " loaded! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Loaded");
        }
    }

    private void setupFromFile(File file) {
        String contents = FileUtils.contentsToString(file);
        JSONArray items = null;
        JSONObject metadata = null;
        try {
            JSONObject content = new JSONObject(contents);
            metadata = content.optJSONObject("metadata");
            items = content.optJSONArray("items");
        } catch (JSONException ignore) {
            try {
                items = new JSONArray(contents);
            } catch (JSONException ignore2) {
                Triton.get().logWarning("Failed to load translations from file %1! Make sure it has a valid JSON " +
                        "syntax.", file.getName());
            }
        }
        setup(metadata, items, com.google.common.io.Files.getNameWithoutExtension(file.getName()));
    }

    private void setup(JSONObject metadata, JSONArray raw, String fileName) {
        if (metadata == null) metadata = new JSONObject();
        boolean defaultUniversal = metadata.optBoolean("universal", true);
        boolean defaultBlacklist = metadata.optBoolean("blacklist", false);
        JSONArray defaultServers = metadata.optJSONArray("servers");
        for (int i = 0; i < raw.length(); i++) {
            JSONObject obj = raw.optJSONObject(i);
            if (Triton.isBungee()) {
                if (!obj.has("universal")) obj.put("universal", defaultUniversal);
                if (!obj.has("blacklist")) obj.put("blacklist", defaultBlacklist);
                if (!obj.has("servers")) obj.put("servers", defaultServers);
            }
            obj.put("fileName", fileName);
            LanguageItem item = LanguageItem.fromJSON(obj);
            if (item == null) continue;
            items.add(item);
        }
    }

    private void logCount(long timeStarted, String action) {
        Triton.get().logDebug(action + " %1 translation items in %2 ms!", items.size(),
                System.currentTimeMillis() - timeStarted);
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
                    obj.put("matches", lt.getMatches());
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
            File file = new File(Triton.get().getDataFolder(), "translations.cache.json");
            Files.write(file.toPath(), array.toString(4).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            Triton.get().logWarning("An error occurred while saving translations items to cache! Some items may not " +
                    "be " +
                    "saved if there is a server shutdown! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Saved");
        }
    }
}
