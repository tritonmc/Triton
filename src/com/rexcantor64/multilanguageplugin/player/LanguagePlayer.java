package com.rexcantor64.multilanguageplugin.player;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.storage.PlayerStorage;
import org.bukkit.entity.Player;

public class LanguagePlayer {

    private final Player bukkit;

    private boolean waitingForDefaultLanguage = false;
    private Language lang;

    public LanguagePlayer(Player p) {
        this.bukkit = p;
        load();
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        this.lang = lang;
        refreshScoreboard();
        refreshEntities();
        refreshSigns();
        save();
    }

    public void waitForDefaultLanguage() {
        waitingForDefaultLanguage = true;
    }

    public boolean isWaitingForDefaultLanguage() {
        return waitingForDefaultLanguage;
    }

    private void refreshSigns() {
        if (!SpigotMLP.get().getConf().isSigns())
            return;
        //TODO implement
    }

    private void refreshScoreboard() {
        if (!SpigotMLP.get().getConf().isScoreboards())
            return;
        //TODO implement
    }

    private void refreshEntities() {
        if (SpigotMLP.get().getConf().getHolograms().size() == 0)
            return;
        //TODO implement
    }

    private void load() {
        setLang(PlayerStorage.StorageManager.getCurrentStorage().getLanguage(this));
    }

    void save() {
        PlayerStorage.StorageManager.getCurrentStorage().setLanguage(bukkit.getUniqueId(), lang);
    }

    public Player toBukkit() {
        return bukkit;
    }

}
