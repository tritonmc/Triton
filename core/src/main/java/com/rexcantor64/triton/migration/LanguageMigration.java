package com.rexcantor64.triton.migration;

import com.google.gson.*;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.utils.FileUtils;
import lombok.Cleanup;
import lombok.val;
import lombok.var;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

public class LanguageMigration {

    private static final JsonParser JSON_PARSER = new JsonParser();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void migrate() {
        // Remove universal from language items and collections
        // Change cache.json
        // players.yml to players.json

        val yamlFile = new File(Triton.get().getDataFolder(), "players.yml");
        if (!yamlFile.exists()) return;

        val jsonFile = new File(Triton.get().getDataFolder(), "players.json");
        if (jsonFile.exists()) return;

        Triton.get().getLogger().logInfo("[Migration] Starting migration from Triton v2... No files will be deleted!");
        val start = System.currentTimeMillis();

        try {
            // players.yml to players.json
            val yml = ConfigurationProvider.getProvider(YamlConfiguration.class).load(yamlFile);
            val languageMap = new HashMap<String, String>();
            yml.getKeys().forEach(k -> languageMap.put(k, yml.getString(k)));

            @Cleanup val fileWriter = FileUtils.getWriterFromFile(jsonFile);
            gson.toJson(languageMap, fileWriter);

            // cache.json
            if (Triton.isSpigot()) {
                val cacheFile = new File(Triton.get().getDataFolder(), "cache.json");
                if (cacheFile.exists()) {
                    val element = JSON_PARSER.parse(FileUtils.getReaderFromFile(cacheFile));
                    if (element.isJsonObject()) {
                        val obj = element.getAsJsonObject();
                        if (!obj.has("languages")) {
                            val result = new JsonObject();
                            val resultLanguages = new JsonArray();
                            String mainLanguage = "";
                            for (val entry : obj.entrySet()) {
                                val entryObj = entry.getValue().getAsJsonObject();
                                if (entryObj.get("main").getAsBoolean())
                                    mainLanguage = entry.getKey();
                                val newObj = new JsonObject();
                                newObj.addProperty("name", entry.getKey());
                                newObj.add("minecraftCodes", entryObj.getAsJsonArray("minecraft-code"));
                                newObj.add("rawDisplayName", entryObj.get("display-name"));
                                newObj.add("flagCode", entryObj.get("flag"));
                                newObj.add("cmds", new JsonArray());
                                resultLanguages.add(newObj);
                            }
                            result.addProperty("mainLanguage", mainLanguage);
                            result.add("languages", resultLanguages);

                            @Cleanup val cacheFileWriter = FileUtils.getWriterFromFile(cacheFile);
                            gson.toJson(result, cacheFileWriter);
                        }
                    }
                }
            }

            val translationsFolder = Triton.get().getTranslationsFolder();
            if (translationsFolder.isDirectory()) {
                for (val file : Objects.requireNonNull(translationsFolder.listFiles())) {
                    if (!file.getName().endsWith(".json")) continue;

                    val fileContent = JSON_PARSER.parse(FileUtils.getReaderFromFile(file));
                    boolean collectionUniversal = false;
                    JsonArray items;
                    if (fileContent.isJsonObject()) {
                        items = fileContent.getAsJsonObject().getAsJsonArray("items");
                        val metadata = fileContent.getAsJsonObject().getAsJsonObject("metadata");
                        if (metadata.has("universal")) {
                            if (metadata.getAsJsonPrimitive("universal").getAsBoolean()) {
                                metadata.addProperty("blacklist", true);
                                metadata.add("servers", new JsonArray());
                                collectionUniversal = true;
                            }
                            metadata.remove("universal");
                        }
                    } else {
                        items = fileContent.getAsJsonArray();
                    }

                    if (items != null) {
                        val finalCollectionUniversal = collectionUniversal;
                        items.forEach((element -> {
                            val item = element.getAsJsonObject();
                            val universal = item.getAsJsonPrimitive("universal");
                            if ((universal == null && finalCollectionUniversal) || (universal != null && universal
                                    .getAsBoolean())) {
                                item.remove("blacklist");
                                item.remove("servers");
                                item.remove("universal");
                            }
                        }));
                    }

                    @Cleanup val translationFileWriter = FileUtils.getWriterFromFile(file);
                    gson.toJson(fileContent, translationFileWriter);
                }
            }

            Triton.get().getLogger()
                    .logInfo("[Migration] Finished successfully in %1 ms!", System.currentTimeMillis() - start);
            Triton.get().getLogger()
                    .logInfo("[Migration] Feel free to delete players.yml!");
        } catch (JsonParseException e) {
            Triton.get().getLogger().logError("[Migration] Aborting... Failed to load some json file: bad syntax. %1",
                    e.getMessage());
            e.printStackTrace();
        } catch (IOException | SecurityException e) {
            Triton.get().getLogger()
                    .logError("[Migration] Aborting... Failed to read from or write to files: %1", e.getMessage());
            e.printStackTrace();
        }
    }

}
