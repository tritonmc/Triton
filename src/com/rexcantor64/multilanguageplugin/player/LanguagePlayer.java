package com.rexcantor64.multilanguageplugin.player;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.packetinterceptor.PacketInterceptor;
import com.rexcantor64.multilanguageplugin.storage.PlayerStorage;
import org.bukkit.entity.Player;

public class LanguagePlayer {

    private final Player bukkit;

    private boolean waitingForDefaultLanguage = false;
    private Language lang;

    private PacketInterceptor interceptor;

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
        bukkit.updateInventory();
        save();
    }

    public void waitForDefaultLanguage() {
        waitingForDefaultLanguage = true;
    }

    public boolean isWaitingForDefaultLanguage() {
        return waitingForDefaultLanguage;
    }

    public void setInterceptor(PacketInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    private void refreshSigns() {
        if (!MultiLanguagePlugin.get().getConf().isSigns())
            return;
        if (interceptor != null)
            interceptor.refreshSigns(this);
    }

    private void refreshScoreboard() {
        if (!MultiLanguagePlugin.get().getConf().isScoreboards())
            return;
        //TODO implement
    }

    private void refreshEntities() {
        if (MultiLanguagePlugin.get().getConf().getHolograms().size() == 0)
            return;
        if (interceptor != null)
            interceptor.refreshEntities(this);
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
