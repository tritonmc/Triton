package com.rexcantor64.triton.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.packetinterceptor.PacketInterceptor;
import com.rexcantor64.triton.scoreboard.WrappedScoreboard;
import com.rexcantor64.triton.scoreboard.bridge.ProtocolLibBridge;
import com.rexcantor64.triton.storage.PlayerStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpigotLanguagePlayer implements LanguagePlayer {

    private final UUID uuid;
    private Player bukkit;

    private Language lang;

    private PacketInterceptor interceptor;

    private String lastTabHeader;
    private String lastTabFooter;
    private HashMap<UUID, String> bossBars = new HashMap<>();

    private WrappedScoreboard scoreboard;

    public SpigotLanguagePlayer(UUID p) {
        uuid = p;
        load();
        scoreboard = new WrappedScoreboard(new ProtocolLibBridge(this), this);
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
        if (sendToBungee && Triton.asSpigot().getBridgeManager() != null)
            Triton.asSpigot().getBridgeManager().updatePlayerLanguage(this);
        executeCommands();
    }

    public WrappedScoreboard getScoreboard() {
        return scoreboard;
    }

    public void refreshAll() {
        if (toBukkit() == null)
            return;
        refreshScoreboard();
        refreshEntities();
        refreshSigns();
        toBukkit().updateInventory();
        if (interceptor != null && Triton.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
            interceptor.refreshTabHeaderFooter(this, lastTabHeader, lastTabFooter);
        if (interceptor != null && Triton.get().getConf().isBossbars())
            for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                interceptor.refreshBossbar(this, entry.getKey(), entry.getValue());
    }

    public void setInterceptor(PacketInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    private void refreshSigns() {
        if (!Triton.get().getConf().isSigns())
            return;
        if (interceptor != null)
            interceptor.refreshSigns(this);
    }

    private void refreshScoreboard() {
        if (!Triton.get().getConf().isScoreboards())
            return;
        interceptor.refreshScoreboard(this);
    }

    private void refreshEntities() {
        if (Triton.get().getConf().getHolograms().size() == 0 && !Triton.get().getConf().isHologramsAll())
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
        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            executeCommands();
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

    private void executeCommands() {
        for (ExecutableCommand cmd : ((com.rexcantor64.triton.language.Language) lang).getCmds()) {
            String cmdText = cmd.getCmd().replace("%player%", bukkit.getName()).replace("%uuid%", bukkit.getUniqueId().toString());
            if (cmd.getType() == ExecutableCommand.Type.SERVER)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdText);
            else if (cmd.getType() == ExecutableCommand.Type.PLAYER)
                Bukkit.dispatchCommand(bukkit, cmdText);
        }
    }
}
