package com.rexcantor64.triton.storage;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.player.LanguagePlayer;

import java.io.File;
import java.util.UUID;

public class YamlStorage implements PlayerStorage {

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
        try {
            Triton.get().logDebug("Saving language for %1...", uuid.toString());
            Configuration config = Triton.get().loadYAML("players", "players");
            config.set(uuid.toString(), newLanguage.getName());
            if (ip != null)
                config.set(ip.replace(".", "-"), newLanguage.getName());
            ConfigurationProvider.getProvider(com.rexcantor64.triton.config.interfaces.YamlConfiguration.class)
                    .save(config,
                            new File(Triton.get().getDataFolder(), "players.yml"));
            Triton.get().logDebug("Saved!");
        } catch (Exception e) {
            Triton.get().logError("Failed to save language for %1! Could not create players.yml: %2", uuid
                    .toString(), e.getMessage());
        }
    }

    private String getValueFromStorage(String key) {
        Configuration config = Triton.get().loadYAML("players", "players");
        return config.getString(key, null);
    }

}
