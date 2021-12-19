package com.rexcantor64.triton.player;

import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.events.PlayerChangeLanguageSpigotEvent;
import com.rexcantor64.triton.api.language.Language;
import com.rexcantor64.triton.language.ExecutableCommand;
import com.rexcantor64.triton.packetinterceptor.PacketInterceptor;
import com.rexcantor64.triton.storage.LocalStorage;
import lombok.Data;
import lombok.Getter;
import lombok.var;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpigotLanguagePlayer implements LanguagePlayer {

    private final UUID uuid;
    private Player bukkit;

    private Language lang;

    private String lastTabHeader;
    private String lastTabFooter;
    private Map<UUID, String> bossBars = new ConcurrentHashMap<>();
    private boolean waitingForClientLocale = false;

    @Getter
    private Map<World, Map<Integer, Optional<String>>> entitiesMap = new ConcurrentHashMap<>();
    @Getter
    private Map<World, Map<Integer, Entity>> playersMap = new ConcurrentHashMap<>();
    @Getter
    private Set<UUID> shownPlayers = new HashSet<>();
    @Getter
    private Map<String, ScoreboardObjective> objectivesMap = new ConcurrentHashMap<>();
    @Getter
    private Map<String, ScoreboardTeam> teamsMap = new ConcurrentHashMap<>();

    public SpigotLanguagePlayer(UUID p) {
        uuid = p;
        load();
    }

    public void setScoreboardObjective(String name, String chatJson, Object type) {
        var objective = this.objectivesMap.computeIfAbsent(name, k -> new ScoreboardObjective());
        objective.setChatJson(chatJson);
        objective.setType(type);
    }

    public void removeScoreboardObjective(String name) {
        this.objectivesMap.remove(name);
    }

    public void setScoreboardTeam(String name, String displayJson, String prefixJson, String suffixJson,
                                  List<Object> optionData) {
        var objective = this.teamsMap.computeIfAbsent(name, k -> new ScoreboardTeam());
        objective.setDisplayJson(displayJson);
        objective.setPrefixJson(prefixJson);
        objective.setSuffixJson(suffixJson);
        objective.setOptionData(optionData);
    }

    public void removeScoreboardTeam(String name) {
        this.teamsMap.remove(name);
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
        if (this.waitingForClientLocale) {
            try {
                if (toBukkit() != null)
                    bukkit.sendMessage(ChatColor.translateAlternateColorCodes('&', Triton.get().getMessagesConfig()
                            .getMessage("success.detected-language", lang.getDisplayName())));
                else
                    Triton.get().getLogger()
                            .logError(1, "Could not automatically set language for %1 because Bukkit Player instance " +
                                    "is null", uuid);
            } catch (Exception e) {
                Triton.get().getLogger().logError("Failed to send \"language changed\" message.");
                e.printStackTrace();
            }
        }
        this.lang = event.getNewLanguage();
        this.waitingForClientLocale = false;
        refreshAll();
        if (Triton.asSpigot().getBridgeManager() == null || Triton.get().getStorage() instanceof LocalStorage)
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

    private Optional<PacketInterceptor> getInterceptor() {
        return Optional.ofNullable(Triton.asSpigot().getProtocolLibListener());
    }

    public void refreshAll() {
        if (toBukkit() == null)
            return;
        refreshEntities();
        refreshSigns();
        toBukkit().updateInventory();
        getInterceptor().ifPresent((interceptor) -> {
            if (Triton.get().getConf().isTab() && lastTabHeader != null && lastTabFooter != null)
                interceptor.refreshTabHeaderFooter(this, lastTabHeader, lastTabFooter);
            if (Triton.get().getConf().isBossbars())
                for (Map.Entry<UUID, String> entry : bossBars.entrySet())
                    interceptor.refreshBossbar(this, entry.getKey(), entry.getValue());
            if (Triton.get().getConfig().isScoreboards())
                interceptor.refreshScoreboard(this);
        });
    }

    private void refreshSigns() {
        if (!Triton.get().getConf().isSigns())
            return;
        getInterceptor().ifPresent(interceptor -> interceptor.refreshSigns(this));
    }

    private void refreshEntities() {
        if (Triton.get().getConf().getHolograms().size() == 0 && !Triton.get().getConf().isHologramsAll())
            return;
        getInterceptor().ifPresent(interceptor -> interceptor.refreshEntities(this));
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
        lang = Triton.get().getStorage().getLanguage(this);
        if (toBukkit() != null)
            Triton.get().getStorage()
                    .setLanguage(null, toBukkit().getAddress().getAddress().getHostAddress(), lang);
        if (lang != null)
            Bukkit.getScheduler().runTask(Triton.asSpigot().getLoader(), this::refreshAll);
        if (Triton.get().getConf().isRunLanguageCommandsOnLogin())
            executeCommands();
    }

    private void save() {
        Bukkit.getScheduler().runTaskAsynchronously(Triton.asSpigot().getLoader(), () -> {
            String ip = null;
            if (toBukkit() != null)
                ip = bukkit.getAddress().getAddress().getHostAddress();
            Triton.get().getStorage().setLanguage(uuid, ip, lang);
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
        if (toBukkit() == null)
            return;
        for (ExecutableCommand cmd : ((com.rexcantor64.triton.language.Language) lang).getCmds()) {
            String cmdText = cmd.getCmd().replace("%player%", bukkit.getName()).replace("%uuid%",
                    bukkit.getUniqueId().toString());
            if (cmd.getType() == ExecutableCommand.Type.SERVER)
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdText);
            else if (cmd.getType() == ExecutableCommand.Type.PLAYER)
                Bukkit.dispatchCommand(bukkit, cmdText);
        }
    }

    @Data
    public static class ScoreboardObjective {
        private String chatJson;
        private Object type;
    }

    @Data
    public static class ScoreboardTeam {
        private String displayJson;
        private String prefixJson;
        private String suffixJson;
        private List<Object> optionData;
    }

}
