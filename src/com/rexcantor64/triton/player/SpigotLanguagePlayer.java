package com.rexcantor64.triton.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageSpigotEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.packetinterceptor.PacketInterceptor;
import com.rexcantor64.triton.scoreboard.WrappedScoreboard;
import com.rexcantor64.triton.scoreboard.bridge.ProtocolLibBridge;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
    private boolean waitingForClientLocale = false;

    private WrappedScoreboard scoreboard;

    private HashMap<World, HashMap<Integer, String>> entities = new HashMap<>();
    private HashMap<World, HashMap<Integer, Entity>> players = new HashMap<>();

    public SpigotLanguagePlayer(UUID p) {
        uuid = p;
        load();
        scoreboard = new WrappedScoreboard(new ProtocolLibBridge(this), this);
    }

    public Language getLang() {
        if (lang == null)
            lang = Triton.get().getLanguageManager().getMainLanguage();
        return lang;
    }

    public void setLang(Language lang) {
        setLang(lang, true);
    }

    public void setLang(Language lang, boolean sendToBungee) {
        PlayerChangeLanguageSpigotEvent event = new PlayerChangeLanguageSpigotEvent(this, this.lang, lang);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        if (this.waitingForClientLocale)
            try {
                toBukkit().sendMessage(Triton.get().getMessage("success.detected-language",
                        "&aYour language has been automatically set to %1", lang.getDisplayName()));
            } catch (Exception e) {
                Triton.get().logError("Failed to sent language changed message.");
                e.printStackTrace();
            }
        this.lang = event.getNewLanguage();
        this.waitingForClientLocale = false;
        refreshAll();
        save();
        if (sendToBungee && Triton.asSpigot().getBridgeManager() != null)
            Triton.asSpigot().getBridgeManager().updatePlayerLanguage(this);
        executeCommands();
    }

    @Override
    public boolean isWaitingForClientLocale() {
        return waitingForClientLocale;
    }

    public void waitForClientLocale() {
        this.waitingForClientLocale = true;
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
        Bukkit.getScheduler().runTaskAsynchronously(Triton.get().getLoader().asSpigot(), () -> {
            lang = Triton.get().getPlayerStorage().getLanguage(this);
            if (toBukkit() != null)
                Triton.get().getPlayerStorage()
                        .setLanguage(null, toBukkit().getAddress().getAddress().getHostAddress(), lang);
            if (lang != null)
                Bukkit.getScheduler().runTask(Triton.get().getLoader().asSpigot(), this::refreshAll);
            if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
                executeCommands();
        });
    }

    private void save() {
        Bukkit.getScheduler().runTaskAsynchronously(Triton.get().getLoader().asSpigot(), () -> {
            String ip = null;
            if (toBukkit() != null)
                ip = toBukkit().getAddress().getAddress().getHostAddress();
            Triton.get().getPlayerStorage().setLanguage(uuid, ip, lang);
        });
    }

    public Player toBukkit() {
        if (bukkit != null && !bukkit.isOnline())
            bukkit = null;
        if (bukkit == null)
            bukkit = Bukkit.getPlayer(uuid);
        return bukkit;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    private void executeCommands() {
        for (ExecutableCommand cmd : ((com.rexcantor64.triton.language.Language) lang).getCmds()) {
            String cmdText = cmd.getCmd().replace("%player%", bukkit.getName()).replace("%uuid%",
                    bukkit.getUniqueId().toString());
            if (cmd.getType() == ExecutableCommand.Type.SERVER)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdText);
            else if (cmd.getType() == ExecutableCommand.Type.PLAYER)
                Bukkit.dispatchCommand(bukkit, cmdText);
        }
    }

    public HashMap<World, HashMap<Integer, String>> getEntitiesMap() {
        return entities;
    }

    public HashMap<World, HashMap<Integer, Entity>> getPlayersMap() {
        return players;
    }
}
