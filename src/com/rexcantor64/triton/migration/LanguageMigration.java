package com.rexcantor64.triton.migration;

import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.utils.LocationUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LanguageMigration {

    private static final Pattern filePattern = Pattern.compile("(.+)\\.(?:language|signs\\.json)");

    private JSONArray languageArray = new JSONArray();

    public static void migrate() {
        File file = new File(MultiLanguagePlugin.get().getDataFolder(), "languages.json");
        if (file.exists()) return;
        File languageFolder = MultiLanguagePlugin.get().getLanguageFolder();
        if (!languageFolder.exists()) return;
        String[] files = languageFolder.list();
        if (files == null) return;
        MultiLanguagePlugin.get().logInfo("Starting migration from legacy versions... No files will be deleted!");
        HashSet<String> languageFiles = new HashSet<>();
        for (String f : files) {
            Matcher matcher = filePattern.matcher(f);
            if (matcher.matches())
                languageFiles.add(matcher.group(1));
        }
        if (languageFiles.isEmpty()) {
            MultiLanguagePlugin.get().logInfo("No legacy files found! Migration aborted.");
            return;
        }
        LanguageMigration migration = new LanguageMigration(languageFiles);
        if (migration.finishMigration())
            MultiLanguagePlugin.get().logInfo("Migration finished successfully!");
    }

    private LanguageMigration(HashSet<String> languageList) {
        for (String s : languageList) {
            addLanguage(s);
            addLanguageSign(s);
        }
    }

    private void addLanguage(String languageName) {
        try {
            MultiLanguagePlugin.get().logDebug("Migrating %1.language to the new system...", languageName);
            File file = new File(MultiLanguagePlugin.get().getLanguageFolder(), languageName + ".language");
            if (!file.exists()) return;
            ResourceBundle rb = new PropertyResourceBundle(new FileReader(file));
            for (String s : Collections.list(rb.getKeys())) addMessageToArray(languageName, s, rb.getString(s));
            MultiLanguagePlugin.get().logDebug("Successfully migrated %1.language to the new system!", languageName);
        } catch (Exception e) {
            MultiLanguagePlugin.get().logError("Failed to migrate %1.language to the new system: %2", languageName, e.getMessage());
        }
    }

    private void addLanguageSign(String languageName) {
        try {
            MultiLanguagePlugin.get().logDebug("Migrating %1.signs.json to the new system...", languageName);
            File file = new File(MultiLanguagePlugin.get().getLanguageFolder(), languageName + ".signs.json");
            if (!file.exists()) return;
            JSONArray signs = new JSONArray(IOUtils.toString(new FileReader(file)));
            for (int i = 0; i < signs.length(); i++) {
                JSONObject sign = signs.getJSONObject(i);
                JSONArray linesJson = sign.optJSONArray("lines");
                String[] lines = new String[4];
                if (linesJson != null)
                    for (int k = 0; k < 4; k++)
                        if (linesJson.length() > k)
                            lines[k] = linesJson.optString(k, "");
                        else
                            lines[k] = "";
                addSignToArray(languageName, sign.optInt("x", 0), sign.optInt("y", 0), sign.optInt("z", 0), sign.optString("world", "world"), lines);
            }
            MultiLanguagePlugin.get().logDebug("Successfully migrated %1.signs.json to the new system!", languageName);
        } catch (Exception e) {
            MultiLanguagePlugin.get().logError("Failed to migrate %1.signs.json to the new system: %2", languageName, e.getMessage());
        }
    }

    private void addMessageToArray(String language, String key, String value) {
        for (int i = 0; i < languageArray.length(); i++) {
            JSONObject entry = languageArray.optJSONObject(i);
            if (entry == null) continue;
            if (!entry.optString("type", "").equalsIgnoreCase("text")) continue;
            if (!entry.optString("key", "").equals(key)) continue;
            JSONObject languages = entry.optJSONObject("languages");
            if (languages == null) {
                languages = new JSONObject();
                entry.put("languages", languages);
            }
            languages.put(language, value);
            return;
        }
        JSONObject entry = new JSONObject();
        entry.put("type", "text");
        entry.put("key", key);
        entry.put("universal", false);
        entry.put("description", "");
        entry.put("tags", new JSONArray());
        JSONObject languages = new JSONObject();
        languages.put(language, value);
        entry.put("languages", languages);
        languageArray.put(entry);
    }

    private void addSignToArray(String language, int x, int y, int z, String world, String[] lines) {
        JSONObject location = LocationUtils.locationToJSON(x, y, z, world);
        for (int i = 0; i < languageArray.length(); i++) {
            JSONObject entry = languageArray.optJSONObject(i);
            if (entry == null) continue;
            if (!entry.optString("type", "").equalsIgnoreCase("sign")) continue;
            JSONObject entryLoc = entry.optJSONObject("location");
            if (entryLoc == null) continue;
            if (!LocationUtils.equalsJSONLocation(entryLoc, location)) continue;
            JSONObject languages = entry.optJSONObject("lines");
            if (languages == null) {
                languages = new JSONObject();
                entry.put("lines", languages);
            }
            languages.put(language, lines);
            return;
        }
        JSONObject entry = new JSONObject();
        entry.put("key", UUID.randomUUID().toString());
        entry.put("type", "sign");
        entry.put("locations", Collections.singleton(location));
        entry.put("description", "");
        entry.put("tags", new JSONArray());
        JSONObject languages = new JSONObject();
        languages.put(language, lines);
        entry.put("lines", languages);
        languageArray.put(entry);
    }

    private boolean finishMigration() {
        try {
            File file = new File(MultiLanguagePlugin.get().getDataFolder(), "languages.json");
            if (file.exists()) {
                MultiLanguagePlugin.get().logDebugWarning("Failed to finish migration! languages.json already exists!");
                return false;
            }
            PrintWriter writer = new PrintWriter(file, "UTF-8");
            writer.print(languageArray.toString(4));
            writer.close();
            return true;
        } catch (Exception e) {
            MultiLanguagePlugin.get().logError("Failed to finish migration (writing out the file): %1", e.getMessage());
            return false;
        }
    }

}
