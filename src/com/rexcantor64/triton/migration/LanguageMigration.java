package com.rexcantor64.triton.migration;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.utils.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class LanguageMigration {

    public static void migrate() {
        File file = new File(Triton.get().getDataFolder(), "languages.json");
        if (!file.exists()) return;
        File translationsFolder = Triton.get().getTranslationsFolder();
        if (translationsFolder.exists()) return;
        Triton.get().logInfo("[Migration] Starting migration from Triton v1... No files will be deleted!");
        try {
            JSONArray items = new JSONArray(FileUtils.contentsToString(file));
            if (!translationsFolder.mkdir()) {
                Triton.get().logError("[Migration] Aborting... Failed to create the translations folder. Make sure " +
                        "the server has enough write permissions.");
                return;
            }
            File defaultFile = new File(translationsFolder, "default.json");

            if (Triton.isBungee()) {
                JSONObject defaultData = new JSONObject();
                JSONObject metadataDefault = new JSONObject();
                metadataDefault.put("universal", true);
                metadataDefault.put("servers", new JSONArray());
                metadataDefault.put("blacklist", false);
                defaultData.put("metadata", metadataDefault);
                defaultData.put("items", items);
                Files.write(defaultFile.toPath(), defaultData.toString(2).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } else {
                Files.write(defaultFile.toPath(), items.toString(2).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
            Triton.get().logInfo("[Migration] Finished successfully!");
            Triton.get().logInfo("[Migration] Feel free to delete languages.json and languages.cache.json!");
        } catch (JSONException e) {
            Triton.get().logError("[Migration] Aborting... Failed to load languages.json: bad syntax. %1",
                    e.getMessage());
        } catch (IOException | SecurityException e) {
            Triton.get().logError("[Migration] Aborting... Failed to create default.json: " + e.getMessage());
        }
    }

}
