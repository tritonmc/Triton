package com.rexcantor64.triton.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.rexcantor64.triton.MultiLanguagePlugin;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.language.Language;
import com.rexcantor64.triton.packetinterceptor.BungeeListener;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BungeeLanguagePlayer implements LanguagePlayer {

    private final ProxiedPlayer parent;

    private Language language;
    private BungeeListener listener;

    private String lastTabHeader;
    private String lastTabFooter;
    private HashMap<UUID, String> bossBars = new HashMap<>();

    public BungeeLanguagePlayer(UUID parent) {
        this.parent = BungeeCord.getInstance().getPlayer(parent);
        load();
    }

    public void setBossbar(UUID uuid, String lastBossBar) {
        bossBars.put(uuid, lastBossBar);
    }

    public void removeBossbar(UUID uuid) {
        bossBars.remove(uuid);
    }

    public void setLastTabHeader(String lastTabHeader) {
        this.lastTabHeader = lastTabHeader;
    }

    public void setLastTabFooter(String lastTabFooter) {
        this.lastTabFooter = lastTabFooter;
    }

    public Language getLang() {
        return language;
    }

    public void setLang(Language language) {
        this.language = language;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        // Action 1
        out.writeByte(1);
        out.writeUTF(parent.getUniqueId().toString());
        out.writeUTF(language.getName());
        parent.getServer().sendData("triton:main", out.toByteArray());
        save();
        refreshAll();
    }

    private void refreshAll() {
        if (listener == null) return;
        listener.refreshTab();
        if (MultiLanguagePlugin.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
            listener.refreshTabHeaderFooter(lastTabHeader, lastTabFooter);
        if (MultiLanguagePlugin.get().getConf().isBossbars())
            for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                listener.refreshBossbar(entry.getKey(), entry.getValue());
    }

    public ProxiedPlayer getParent() {
        return parent;
    }

    private void load() {
        Configuration config = MultiLanguagePlugin.get().loadYAML("players", "players");
        language = MultiLanguagePlugin.get().getLanguageManager().getLanguageByName(config.getString(parent.getUniqueId().toString()), true);
    }

    private void save() {
        Configuration config = MultiLanguagePlugin.get().loadYAML("players", "players");
        config.set(parent.getUniqueId().toString(), language.getName());
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, new File(MultiLanguagePlugin.get().getDataFolder(), "players.yml"));
        } catch (IOException e) {
            MultiLanguagePlugin.get().logError("Failed to save players.yml: %1", e.getMessage());
        }
    }

    public void setListener(BungeeListener listener) {
        this.listener = listener;
    }
}
