package com.rexcantor64.multilanguageplugin.player;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.config.interfaces.Configuration;
import com.rexcantor64.multilanguageplugin.config.interfaces.ConfigurationProvider;
import com.rexcantor64.multilanguageplugin.config.interfaces.YamlConfiguration;
import com.rexcantor64.multilanguageplugin.language.Language;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.io.IOException;

public class BungeeLanguagePlayer {

    private final ProxiedPlayer parent;

    private Language language;

    public BungeeLanguagePlayer(ProxiedPlayer parent) {
        this.parent = parent;
        load();
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
        save();
    }

    public ProxiedPlayer getParent() {
        return parent;
    }

    private void load() {
        Configuration config = MultiLanguagePlugin.get().loadYAML("players");
        language = MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(config.getString(parent.getUniqueId().toString()), true);
    }

    private void save() {
        Configuration config = MultiLanguagePlugin.get().loadYAML("players");
        config.set(parent.getUniqueId().toString(), language.getName());
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(MultiLanguagePlugin.get().getDataFolder(), "players.yml"));
        } catch (IOException e) {
            MultiLanguagePlugin.get().logError("Failed to save players.yml: %1", e.getMessage());
        }
    }
}
