package com.rexcantor64.multilanguageplugin.player;

import com.rexcantor64.multilanguageplugin.MultiLanguagePlugin;
import com.rexcantor64.multilanguageplugin.language.Language;
import com.rexcantor64.multilanguageplugin.packetinterceptor.PacketInterceptor;
import com.rexcantor64.multilanguageplugin.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpigotLanguagePlayer implements LanguagePlayer {

    private final UUID uuid;
    private Player bukkit;

    private boolean waitingForDefaultLanguage = false;
    private Language lang;

    private PacketInterceptor interceptor;

    private String lastTabHeader;
    private String lastTabFooter;
    private HashMap<UUID, String> bossBars = new HashMap<>();

    public SpigotLanguagePlayer(UUID p) {
        uuid = p;
        load();
    }

    public Language getLang() {
        return lang;
    }

    public void setLang(Language lang) {
        setLang(lang, true);
    }

    public void setLang(Language lang, boolean sendToBungee) {
        this.lang = lang;
        refreshAll();
        save();
        if (sendToBungee && MultiLanguagePlugin.asSpigot().getBridgeManager() != null)
            MultiLanguagePlugin.asSpigot().getBridgeManager().updatePlayerLanguage(this);
    }

    public void refreshAll() {
        refreshScoreboard();
        refreshEntities();
        refreshSigns();
        toBukkit().updateInventory();
        if (interceptor != null && MultiLanguagePlugin.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
            interceptor.refreshTabHeaderFooter(this, lastTabHeader, lastTabFooter);
        if (interceptor != null && MultiLanguagePlugin.get().getConf().isBossbars())
            for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                interceptor.refreshBossbar(this, entry.getKey(), entry.getValue());
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
        lang = PlayerStorage.StorageManager.getCurrentStorage().getLanguage(this);
    }

    void save() {
        PlayerStorage.StorageManager.getCurrentStorage().setLanguage(uuid, lang);
    }

    public Player toBukkit() {
        if (bukkit == null)
            bukkit = Bukkit.getPlayer(uuid);
        return bukkit;
    }

    public UUID getUUID() {
        return uuid;
    }
}
