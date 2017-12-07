package com.rexcantor64.multilanguageplugin.config;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.item.LanguageItem;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class LanguageConfig {

    private JSONArray raw;
    private List<LanguageItem> items = new ArrayList<>();

    public List<LanguageItem> getItems() {
        return items;
    }

    public JSONArray getRaw() {
        return raw;
    }

    public void setup() {
        items.clear();
        long timeStarted = System.currentTimeMillis();
        try {
            File file = new File(SpigotMLP.get().getDataFolder(), "languages.json");
            if (!file.exists()) {
                try {
                    if (!file.createNewFile())
                        SpigotMLP.get().logDebugWarning("Failed to create %1! File already exists!", file.getAbsolutePath());
                } catch (Exception e) {
                    SpigotMLP.get().logDebugWarning("Failed to create %1! Error: %2", file.getAbsolutePath(), e.getMessage());
                }
                logCount(timeStarted);
                return;
            }
            raw = new JSONArray(IOUtils.toString(new FileReader(file)));
            for (int i = 0; i < raw.length(); i++) {
                LanguageItem item = LanguageItem.fromJSON(raw.optJSONObject(i));
                if (item == null) continue;
                items.add(item);
            }
        } catch (Exception e) {
            SpigotMLP.get().logWarning("An error occurred while loading languages.json! Some language items may not have been loaded! Error: %1", e.getMessage());
        }
        logCount(timeStarted);
    }

    private void logCount(long timeStarted) {
        SpigotMLP.get().logDebug("Loaded %1 language items in %2 ms!", items.size(), System.currentTimeMillis() - timeStarted);
    }

}
