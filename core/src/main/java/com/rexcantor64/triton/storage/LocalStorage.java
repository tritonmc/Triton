package com.rexcantor64.triton.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.SpigotMLP;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.item.Collection;
import com.rexcantor64.triton.language.item.LanguageItem;
import com.rexcantor64.triton.language.item.LanguageText;
import com.rexcantor64.triton.language.item.serializers.CollectionSerializer;
import com.rexcantor64.triton.player.LanguagePlayer;
import com.rexcantor64.triton.utils.FileUtils;
import lombok.Cleanup;
import lombok.val;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalStorage extends Storage {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type HASH_MAP_TYPE = new TypeToken<ConcurrentHashMap<String, String>>() {
    }.getType();
    private ConcurrentHashMap<String, String> languageMap = new ConcurrentHashMap<>();

    @Override
    public void load() {
        loadPlayerData();
        this.collections = downloadFromStorage();
    }

    public void loadPlayerData() {
        val playersFile = new File(Triton.get().getDataFolder(), "players.json");
        if (playersFile.isFile()) {
            try {
                this.languageMap = gson.fromJson(FileUtils.getReaderFromFile(playersFile), HASH_MAP_TYPE);
            } catch (JsonParseException e) {
                Triton.get().getLogger().logError("Failed load players.json. JSON is not valid: %1", e.getMessage());
            }
        }
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
            if (ip != null && Triton.get().getConf().isMotd()) {
                String formattedIp = ip.replace(".", "-");
                if (!newLanguage.getName().equals(languageMap.get(formattedIp))) {
                    languageMap.put(formattedIp, newLanguage.getName());
                    changed = true;
                }
            }

            if (changed) {
                Triton.get().runAsync(() -> {
                    try {
                        // TODO use RandomAccessFile with FileLock if this does not work correctly
                        val playersFile = new File(Triton.get().getDataFolder(), "players.json");
                        @Cleanup val fileWriter = FileUtils.getWriterFromFile(playersFile);
                        gson.toJson(languageMap, fileWriter);
                    } catch (Exception e) {
                        Triton.get().getLogger()
                                .logError("Failed to save language for %1! Could not create players.yml: %2", entity, e
                                        .getMessage());
                        e.printStackTrace();
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
            e.printStackTrace();
        }
    }

    @Override
    public boolean uploadToStorage(ConcurrentHashMap<String, Collection> collections) {

        // Use translations.cache.json
        if (Triton.get().getConf().isBungeecord() && Triton.get() instanceof SpigotMLP) {
            Triton.get().getLogger().logInfo(2, "Saving translations to cache since bungeecord mode is enabled.");

            val cacheFile = new File(Triton.get().getDataFolder(), "translations.cache.json");

            val collection = new Collection();
            collections.values().forEach((col) -> collection.getItems().addAll(col.getItems()));

            try {
                Triton.get().getLogger().logInfo(2, "Saving translations.cache.json");

                @Cleanup val fileWriter = FileUtils.getWriterFromFile(cacheFile);
                CollectionSerializer.toJson(collection, fileWriter);
            } catch (Exception e) {
                Triton.get().getLogger().logError("Failed to save translations.cache.json: %1", e.getMessage());
                e.printStackTrace();
            }
            return true;
        }

        Triton.get().getLogger().logInfo(2, "Saving collections to local storage...");
        val translationsFolder = new File(Triton.get().getDataFolder(), "translations");
        if (!translationsFolder.exists())
            if (!translationsFolder.mkdirs()) {
                Triton.get().getLogger().logError("Couldn't create translations folder to save collections.");
                return false;
            }

        if (!translationsFolder.isDirectory()) {
            Triton.get().getLogger()
                    .logError("There is a file name 'translations' in the Triton folder that is not a folder.");
            return false;
        }

        for (val file : Objects.requireNonNull(translationsFolder.listFiles())) {
            if (!file.isDirectory() && file.getName().endsWith(".json") && !collections
                    .containsKey(file.getName().substring(0, file.getName().length() - 5)))
                if (!file.delete())
                    Triton.get().getLogger()
                            .logError("Failed to delete translations/%1. Some additional translations might be loaded" +
                                    " from local storage in the future.", file.getName());
                else
                    Triton.get().getLogger().logInfo(2, "Deleted translations/%1", file.getName());
        }

        AtomicBoolean success = new AtomicBoolean(true);

        collections.forEach((key, value) -> {
            try {
                Triton.get().getLogger().logInfo(2, "Saving translations/%1.json", key);
                val collectionFile = new File(translationsFolder, key + ".json");
                @Cleanup val fileWriter = FileUtils.getWriterFromFile(collectionFile);
                CollectionSerializer.toJson(value, fileWriter);
            } catch (Exception e) {
                success.set(false);
                Triton.get().getLogger()
                        .logError("Failed to save collection %1.json: %2", key, e
                                .getMessage());
                e.printStackTrace();
            }
        });
        return success.get();
    }

    @Override
    public boolean uploadPartiallyToStorage(ConcurrentHashMap<String, Collection> collections,
                                            List<LanguageItem> changed, List<LanguageItem> deleted) {
        return uploadToStorage(collections);
    }

    @Override
    public ConcurrentHashMap<String, Collection> downloadFromStorage() {
        val collections = new ConcurrentHashMap<String, Collection>();

        // Use translations.cache.json
        if (Triton.get().getConf().isBungeecord() && Triton.get() instanceof SpigotMLP) {
            Triton.get().getLogger().logInfo(2, "Loading translations from cache since bungeecord mode is enabled.");

            val cacheFile = new File(Triton.get().getDataFolder(), "translations.cache.json");

            if (!cacheFile.isFile()) {
                Triton.get().getLogger()
                        .logInfo(2, "Did not load translations from cache because cache file does not exist.");
                return collections;
            }

            collections.put("cache", CollectionSerializer.parse(FileUtils.getReaderFromFile(cacheFile)));

            return collections;
        }

        val translationsFolder = new File(Triton.get().getDataFolder(), "translations");
        if (translationsFolder.isDirectory()) {
            val colFiles = translationsFolder.listFiles();
            if (colFiles != null)
                for (val colFile : colFiles) {
                    try {
                        if (colFile.getName().endsWith(".json"))
                            collections.put(colFile.getName().substring(0, colFile.getName().length() - 5),
                                    CollectionSerializer.parse(FileUtils.getReaderFromFile(colFile)));
                        else
                            Triton.get().getLogger()
                                    .logWarning(2, "Did not load file %1 because it is not a JSON file.", colFile
                                            .getName());
                    } catch (JsonParseException e) {
                        Triton.get().getLogger()
                                .logError("Failed to load collection %1 because it has invalid syntax: %2", colFile
                                        .getName(), e.getMessage());
                        e.printStackTrace();
                    }
                }
            else
                Triton.get().getLogger().logWarning(2, "An I/O error occurred while loading the translations folder.");
        } else if (!translationsFolder.exists()) {
            createSampleTranslationsFolder(translationsFolder);
        }
        return collections;
    }

    private void createSampleTranslationsFolder(File translationsFolder) {
        // Create folder with sample collection to guide new users
        if (!translationsFolder.mkdirs()) {
            Triton.get().getLogger().logError("Failed to create 'translations' folder. Check if the server has the required permissions.");
            return;
        }

        Collection sampleCollection = new Collection();
        LanguageText sampleTranslation = new LanguageText();
        sampleTranslation.setKey("example.translation");
        sampleTranslation.setLanguages(new HashMap<>());
        sampleTranslation.getLanguages().put("en_GB", "This is an example translation in English. " +
                "You can use it with the placeholder [lang]example.translation[/lang].");
        sampleTranslation.getLanguages().put("pt_PT", "Isto é uma tradução exemplo em Português. " +
                "Podes usá-la com o placeholder [lang]example.translation[/lang].");
        sampleCollection.getItems().add(sampleTranslation);

        val sampleCollectionFile = new File(translationsFolder, "default.json");

        try {
            Triton.get().getLogger().logInfo(2, "Saving translations/default.json");

            @Cleanup val fileWriter = FileUtils.getWriterFromFile(sampleCollectionFile);
            CollectionSerializer.toJson(sampleCollection, fileWriter);
        } catch (Exception e) {
            Triton.get().getLogger().logError("Failed to save translations/default.json: %1", e.getMessage());
            e.printStackTrace();
        }
    }

    public String toString() {
        return "Local";
    }
}
