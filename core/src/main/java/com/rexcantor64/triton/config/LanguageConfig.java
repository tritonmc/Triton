package com.rexcantor64.triton.config;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageSign;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class LanguageConfig {

    private JSONArray raw = new JSONArray();
    private JSONObject metadataList = new JSONObject();
    private List<LanguageItem> items = new ArrayList<>();

    public List<LanguageItem> getItems() {
        return items;
    }

    public LanguageConfig setItems(List<LanguageItem> items) {
        this.items = items;
        return this;
    }

    public LanguageConfig addItems(List<LanguageItem> items) {
        this.items.addAll(items);
        return this;
    }

    public JSONArray getRaw() {
        return raw;
    }

    public JSONObject getMetadataList() {
        return metadataList;
    }

    public void setup(boolean useCache) {
        items.clear();
        raw = new JSONArray();
        metadataList = new JSONObject();
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
                    Triton.get().getLogger().logDebug("Found %1 translation files.", files.length);
                    for (File f : files)
                        if (f.getName().endsWith(".json"))
                            setupFromFile(f);
                }
            }
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logWarning("An error occurred while loading translations! Some translation items may not " +
                            "have been" +
                            " loaded! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Loaded", items.size());
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
                Triton.get().getLogger()
                        .logWarning("Failed to load translations from file %1! Make sure it has a valid JSON " +
                                "syntax.", file.getName());
            }
        }
        setup(metadata, items, com.google.common.io.Files.getNameWithoutExtension(file.getName()));
    }

    private void setup(JSONObject metadata, JSONArray raw, String fileName) {
        if (metadata == null) metadata = new JSONObject();
        metadataList.put(fileName, metadata);
        boolean defaultUniversal = metadata.optBoolean("universal", true);
        boolean defaultBlacklist = metadata.optBoolean("blacklist", false);
        JSONArray defaultServers = metadata.optJSONArray("servers");
        for (int i = 0; i < raw.length(); i++) {
            JSONObject obj = raw.optJSONObject(i);
            obj.put("fileName", fileName);
            this.raw.put(obj);
            LanguageItem item = LanguageItem.fromJSON(obj, defaultUniversal, defaultBlacklist, defaultServers);
            if (item == null) continue;
            items.add(item);
        }
    }

    public void saveFromRaw(JSONArray raw) {
        saveFromRaw(raw, metadataList);
    }

    public void saveFromRaw(JSONArray raw, JSONObject metadataList) {
        if (metadataList == null) metadataList = new JSONObject();
        long timeStarted = System.currentTimeMillis();
        HashMap<String, JSONArray> fileMap = new HashMap<>();
        for (int i = 0; i < raw.length(); i++) {
            try {
                JSONObject obj = raw.optJSONObject(i);
                if (obj == null) continue;
                obj = new JSONObject(obj, Objects.requireNonNull(JSONObject.getNames(obj)));
                String fileDestination = obj.optString("fileName", "default");
                JSONObject metadata = metadataList.optJSONObject(fileDestination);
                if (metadata != null) {
                    if (obj.has("universal") && metadata.optBoolean("universal", true) == obj.optBoolean("universal"))
                        obj.remove("universal");
                    if (obj.has("blacklist") && metadata.optBoolean("blacklist", true) == obj.optBoolean("blacklist"))
                        obj.remove("blacklist");
                    if (obj.has("servers") && obj.optJSONArray("servers").equals(metadata.optJSONArray("servers")))
                        obj.remove("servers");
                }
                obj.remove("fileName");
                if (!fileMap.containsKey(fileDestination)) fileMap.put(fileDestination, new JSONArray());
                JSONArray target = fileMap.get(fileDestination);
                target.put(obj);
            } catch (NullPointerException ignore) {
                Triton.get().getLogger()
                        .logDebugWarning("NullPointerException while setting up from raw. One or more translation" +
                                " items are empty");
            }
        }
        for (Map.Entry<String, JSONArray> entry : fileMap.entrySet()) {
            try {
                JSONObject metadata = metadataList.optJSONObject(entry.getKey());
                String content;
                if (Triton.isBungee() && metadata != null) {
                    JSONObject obj = new JSONObject();
                    obj.put("metadata", metadata);
                    obj.put("items", entry.getValue());
                    content = obj.toString(2);
                } else {
                    content = entry.getValue().toString(2);
                }
                File file = new File(Triton.get().getTranslationsFolder(), entry.getKey() + ".json");
                Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                items.clear();
            } catch (Exception e) {
                Triton.get().getLogger().logWarning("An error occurred while saving language items ! Some items may " +
                        "not be saved if there is a server shutdown! Error: %1", e.getMessage());
            }
        }
        logCount(timeStarted, "Saved", raw.length());
    }

    private void logCount(long timeStarted, String action, int amount) {
        Triton.get().getLogger().logDebug(action + " %1 translation items in %2 ms!", amount,
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
                    obj.put("patterns", lt.getPatterns());
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
            Triton.get().getLogger()
                    .logWarning("An error occurred while saving translations items to cache! Some items may not " +
                            "be " +
                            "saved if there is a server shutdown! Error: %1", e.getMessage());
        } finally {
            logCount(timeStarted, "Saved", items.size());
        }
    }
}
