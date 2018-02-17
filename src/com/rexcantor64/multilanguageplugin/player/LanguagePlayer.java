package com.rexcantor64.multilanguageplugin.player;

import com.rexcantor64.multilanguageplugin.SpigotMLP;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.packetinterceptor.PacketInterceptor;
import com.rexcantor64.multilanguageplugin.storage.PlayerStorage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LanguagePlayer {

    private final Player bukkit;

    private boolean waitingForDefaultLanguage = false;
    private Language lang;

    private PacketInterceptor interceptor;

    private String lastTabHeader;
    private String lastTabFooter;
    private HashMap<UUID, String> bossBars = new HashMap<>();

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
        if (interceptor != null && SpigotMLP.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
            interceptor.refreshTabHeaderFooter(this, lastTabHeader, lastTabFooter);
        if (interceptor != null && SpigotMLP.get().getConf().isBossbars())
            for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                interceptor.refreshBossbar(this, entry.getKey(), entry.getValue());
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
        if (!SpigotMLP.get().getConf().isSigns())
            return;
        if (interceptor != null)
            interceptor.refreshSigns(this);
    }

    private void refreshScoreboard() {
        if (!SpigotMLP.get().getConf().isScoreboards())
            return;
        //TODO implement
    }

    private void refreshEntities() {
        if (SpigotMLP.get().getConf().getHolograms().size() == 0)
            return;
        if (interceptor != null)
            interceptor.refreshEntities(this);
    }

    public void setLastTabHeader(String lastTabHeader) {
        this.lastTabHeader = lastTabHeader;
    }

    public void setLastTabFooter(String lastTabFooter) {
        this.lastTabFooter = lastTabFooter;
    }

    public void setBossbar(UUID uuid, String lastBossBar) {
        bossBars.put(uuid, lastBossBar);
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
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
