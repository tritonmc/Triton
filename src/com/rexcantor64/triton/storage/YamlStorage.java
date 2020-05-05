package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.player.LanguagePlayer;

import java.io.File;
import java.util.UUID;

public class YamlStorage implements PlayerStorage {

    private Configuration configuration;

    public YamlStorage() {
        configuration = Triton.get().loadYAML("players", "players");
    }

    @Override
    public Language getLanguageFromIp(String ip) {
        String lang = getValueFromStorage(ip.replace(".", "-"));
        return Triton.get().getLanguageManager().getLanguageByName(lang, true);
    }

    @Override
    public Language getLanguage(LanguagePlayer lp) {
        String lang = getValueFromStorage(lp.getUUID().toString());
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
            Triton.get().logDebug("Saving language for %1...", entity);

            boolean changed = false;
            if (uuid != null) {
                String formattedUuid = uuid.toString();
                if (!newLanguage.getName().equals(configuration.getString(formattedUuid))) {
                    configuration.set(uuid.toString(), newLanguage.getName());
                    changed = true;
                }
            }
            if (ip != null) {
                String formattedIp = ip.replace(".", "-");
                if (!newLanguage.getName().equals(configuration.getString(formattedIp))) {
                    configuration.set(formattedIp, newLanguage.getName());
                    changed = true;
                }
            }

            if (changed) {
                Triton.get().runAsync(() -> {
                    try {
                        ConfigurationProvider
                                .getProvider(com.rexcantor64.triton.config.interfaces.YamlConfiguration.class)
                                .save(configuration,
                                        new File(Triton.get().getDataFolder(), "players.yml"));
                    } catch (Exception e) {
                        Triton.get()
                                .logError("Failed to save language for %1! Could not create players.yml: %2", entity, e
                                        .getMessage());
                    }
                });
                Triton.get().logDebug("Saved!");
            } else {
                Triton.get().logDebug("Skipped saving because there were no changes.");
            }
        } catch (Exception e) {
            Triton.get().logError("Failed to save language for %1! Could not create players.yml: %2", entity, e
                    .getMessage());
        }
    }

    private String getValueFromStorage(String key) {
        return configuration.getString(key, null);
    }

}
