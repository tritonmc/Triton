package com.rexcantor64.triton.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.serializers.CollectionSerializer;
import com.rexcantor64.triton.player.LanguagePlayer;
import lombok.SneakyThrows;
import lombok.val;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalStorage extends Storage {

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type HASH_MAP_TYPE = new TypeToken<ConcurrentHashMap<String, String>>() {
    }.getType();
    private ConcurrentHashMap<String, String> languageMap = new ConcurrentHashMap<>();

    @Override
    public void load() {
        //val languageMap = new ConcurrentHashMap<String, String>();
        val playersFile = new File(Triton.get().getDataFolder(), "players.json");
        if (playersFile.isFile()) {
            try {
                //val element = JSON_PARSER.parse(getReaderFromFile(playersFile));
                // TODO use gson.fromJson
                /*if (element.isJsonObject()) {
                    val obj = element.getAsJsonObject();
                    for (val entry : obj.entrySet())
                        if (entry.getValue().isJsonPrimitive())
                            languageMap.put(entry.getKey(), entry.getValue().getAsString());
                        else
                            Triton.get().getLogger()
                                    .logWarning(2, "[players.json] Entry '%1' is not a string. Ignoring...", entry
                                    .getKey());
                } else
                    Triton.get().getLogger()
                            .logError("players.json does not contain a JSON object. No player data loaded.");*/
                this.languageMap = gson.fromJson(getReaderFromFile(playersFile), HASH_MAP_TYPE);
            } catch (JsonParseException e) {
                Triton.get().getLogger().logError("Failed load players.json. JSON is not valid: %1", e.getMessage());
            }
        }
        //this.languageMap = languageMap;

        val collections = new ConcurrentHashMap<String, Collection>();
        val translationsFolder = new File(Triton.get().getDataFolder(), "translations");
        if (translationsFolder.isDirectory()) {
            val colFiles = translationsFolder.listFiles();
            if (colFiles != null)
                for (val colFile : colFiles) {
                    if (colFile.getName().endsWith(".json"))
                        collections.put(colFile.getName().substring(0, colFile.getName().length() - 5),
                                CollectionSerializer.parse(getReaderFromFile(colFile)));
                    else
                        Triton.get().getLogger()
                                .logWarning(2, "Did not load file %1 because it is not a JSON file.", colFile
                                        .getName());
                }
            else
                Triton.get().getLogger().logWarning(2, "An I/O error occurred while loading the translations folder.");
        }
        this.collections = collections;

    }

    @Override
    public Language getLanguageFromIp(String ip) {
        String lang = languageMap.get(ip.replace(".", "-"));
        return Triton.get().getLanguageManager().getLanguageByName(lang, true);
    }

    @Override
    public Language getLanguage(LanguagePlayer lp) {
        String lang = languageMap.get(lp.getUUID().toString());
        if (!Triton.get().getConf().isBungeecord() &&
                (lang == null
                        || (Triton.get().getConf().isAlwaysCheckClientLocale())))
            lp.waitForClientLocale();
        return Triton.get().getLanguageManager().getLanguageByName(lang, true);
    }

    @Override
    public void setLanguage(UUID uuid, String ip, Language newLanguage) {
        String entity = uuid != null ? uuid.toString() : ip;
        try {
            if (uuid == null && ip == null) return;
            Triton.get().getLogger().logInfo(2, "Saving language for %1...", entity);

            boolean changed = false;
            if (uuid != null) {
                String formattedUuid = uuid.toString();
                if (!newLanguage.getName().equals(languageMap.get(formattedUuid))) {
                    languageMap.put(uuid.toString(), newLanguage.getName());
                    changed = true;
                }
            }
            if (ip != null) {
                String formattedIp = ip.replace(".", "-");
                if (!newLanguage.getName().equals(languageMap.get(formattedIp))) {
                    languageMap.put(formattedIp, newLanguage.getName());
                    changed = true;
                }
            }

            if (changed) {
                Triton.get().runAsync(() -> {
                    try {
                        val playersFile = new File(Triton.get().getDataFolder(), "players.json");
                        gson.toJson(languageMap, new FileWriter(playersFile));
                    } catch (Exception e) {
                        Triton.get().getLogger()
                                .logError("Failed to save language for %1! Could not create players.yml: %2", entity, e
                                        .getMessage());
                    }
                });
                Triton.get().getLogger().logInfo(2, "Saved!");
            } else {
                Triton.get().getLogger().logInfo(2, "Skipped saving because there were no changes.");
            }
        } catch (Exception e) {
            Triton.get().getLogger()
                    .logError("Failed to save language for %1! Could not create players.yml: %2", entity, e
                            .getMessage());
        }
    }

    @SneakyThrows
    private Reader getReaderFromFile(File file) {
        return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
    }

}
