package com.rexcantor64.multilanguageplugin.storage;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.player.SpigotLanguagePlayer;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.UUID;

public interface PlayerStorage {

    Language getLanguage(SpigotLanguagePlayer lp);

    void setLanguage(UUID uuid, Language newLanguage);

    class StorageManager {
        private static final YamlStorage yaml = new YamlStorage();
        private static final SqlStorage sql = new SqlStorage();

        public static PlayerStorage getCurrentStorage() {
            if (MultiLanguagePlugin.get().getConf().useSql())
                return sql;
            return yaml;
        }
    }

    class YamlStorage implements PlayerStorage {

        @Override
        public Language getLanguage(SpigotLanguagePlayer lp) {
            File f = new File(MultiLanguagePlugin.get().getDataFolder(), "players.yml");
            if (!f.exists()) {
                lp.waitForDefaultLanguage();
                return MultiLanguagePlugin.get().getLanguageManager().getMainLanguage();
            }
            YamlConfiguration players = YamlConfiguration.loadConfiguration(f);
            if (players
                    .isString(
                            lp.getUUID()
                                    .toString()))
                return MultiLanguagePlugin.get().getLanguageManager()
                        .getLanguageByName(players.getString(lp.getUUID().toString()), true);
            lp.waitForDefaultLanguage();
            return MultiLanguagePlugin.get().getLanguageManager().getMainLanguage();
        }

        @Override
        public void setLanguage(UUID uuid, Language newLanguage) {
            try {
                MultiLanguagePlugin.get().logDebug("Saving language for %1...", uuid.toString());
                File f = new File(MultiLanguagePlugin.get().getDataFolder(), "players.yml");
                if (!f.exists())
                    if (!f.createNewFile()) {
                        MultiLanguagePlugin.get().logDebugWarning("Failed to save language for %1! Could not create players.yml: File already exists", uuid.toString());
                        return;
                    }
                YamlConfiguration players = YamlConfiguration.loadConfiguration(f);
                players.set(uuid.toString(), newLanguage.getName());
                players.save(f);
                MultiLanguagePlugin.get().logDebug("Saved!");
            } catch (Exception e) {
                MultiLanguagePlugin.get().logDebugWarning("Failed to save language for %1! Could not create players.yml: %2", uuid.toString(), e.getMessage());
            }
        }

    }

    class SqlStorage implements PlayerStorage {

        @Override
        public Language getLanguage(SpigotLanguagePlayer lp) {
            //TODO implement
            return MultiLanguagePlugin.get().getLanguageManager().getMainLanguage();
        }

        @Override
        public void setLanguage(UUID uuid, Language newLanguage) {
            //TODO implement
        }

    }

}
