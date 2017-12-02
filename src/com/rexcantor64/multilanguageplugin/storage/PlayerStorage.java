package com.rexcantor64.multilanguageplugin.storage;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.player.LanguagePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

public interface PlayerStorage {

    Language getLanguage(LanguagePlayer lp);

    void setLanguage(UUID uuid, Language newLanguage);

    class StorageManager {
        private static final YamlStorage yaml = new YamlStorage();
        private static final SqlStorage sql = new SqlStorage();

        public static PlayerStorage getCurrentStorage() {
            if (SpigotMLP.get().getConf().useSql())
                return sql;
            return yaml;
        }
    }

    class YamlStorage implements PlayerStorage {

        @Override
        public Language getLanguage(LanguagePlayer lp) {
            File f = new File(SpigotMLP.get().getDataFolder(), "players.yml");
            if (!f.exists()) {
                lp.waitForDefaultLanguage();
                return SpigotMLP.get().getLanguageManager().getMainLanguage();
            }
            YamlConfiguration players = YamlConfiguration.loadConfiguration(f);
            if (players.isString(lp.toBukkit().getUniqueId().toString()))
                return SpigotMLP.get().getLanguageManager()
                        .getLanguageByName(players.getString(lp.toBukkit().getUniqueId().toString()), true);
            lp.waitForDefaultLanguage();
            return SpigotMLP.get().getLanguageManager().getMainLanguage();
        }

        @Override
        public void setLanguage(UUID uuid, Language newLanguage) {
            try {
                SpigotMLP.get().logDebug("Saving language for %1...", uuid.toString());
                File f = new File(SpigotMLP.get().getDataFolder(), "players.yml");
                if (!f.exists())
                    if (!f.createNewFile()) {
                        SpigotMLP.get().logDebugWarning("Failed to save language for %1! Could not create players.yml: File already exists", uuid.toString());
                        return;
                    }
                YamlConfiguration players = YamlConfiguration.loadConfiguration(f);
                players.set(uuid.toString(), newLanguage.getName());
                players.save(f);
                SpigotMLP.get().logDebug("Saved!");
            } catch (Exception e) {
                SpigotMLP.get().logDebugWarning("Failed to save language for %1! Could not create players.yml: %2", uuid.toString(), e.getMessage());
            }
        }

    }

    class SqlStorage implements PlayerStorage {

        @Override
        public Language getLanguage(LanguagePlayer lp) {
            //TODO implement
            return SpigotMLP.get().getLanguageManager().getMainLanguage();
        }

        @Override
        public void setLanguage(UUID uuid, Language newLanguage) {
            //TODO implement
        }

    }

}
